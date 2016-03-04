package com.hcm.camera.data.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.annotation.SuppressLint;
import android.util.Log;

import com.google.gson.Gson;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.data.ByteBufferManager;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.RTPBuffModel;
import com.hcm.camera.data.VideoGciBuffs;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.ResponseBer;
import com.hcm.camera.net.model.TransDataBase;
import com.hcm.camera.net.model.system.SendHeat;
import com.hcm.camera.net.model.system.SendLog;
import com.hcm.camera.net.model.system.SystemMsgType;
import com.hcm.camera.video.stream.rtp.RtpSocket;

/**
 * Socket处理线程
 * 
 * @author 黃超
 * 
 */
public class NetWorkSocket extends Thread {
	public Socket socket = null;
	public boolean isLostLine = false;
	private OnNetWorkLinstener networklinstener = null;
	private String serverip = "";// "192.168.168.38";
	private int point = 8888;
	private Hashtable<String, Hashtable<String, HashMap<Integer, OnResponseListener>>> reslist = null;
	private HashMap<String, Integer> cmdWorkSeg = new HashMap<String, Integer>();

	public LinkedList<TransDataBase> send_message_queue = new LinkedList<TransDataBase>();
	public List<TransDataBase> re_send_message_queue = new ArrayList<TransDataBase>();

	private NetWorkSocket ower = this;
	private Gson gson = new Gson();
	public boolean isFistStart = false;
	private static final Object sendlock = new Object();

	/**
	 * 读取Socket字节流的缓存对象，封装了协议解析时所�?要的变量
	 */
	private ByteBufferManager buffs = new ByteBufferManager(10, 65565);
	public VideoGciBuffs videoGciBuffs = new VideoGciBuffs(20, RtpSocket.MTU);
	public VideoGciBuffs audioGciBuffs = new VideoGciBuffs(20, 4096);

	private VideoGciBuffs sendVideoGcBuffs = new VideoGciBuffs(10, RtpSocket.MTU);
	private VideoGciBuffs sendAudoGciBuffs = new VideoGciBuffs(10, 4096);

	private byte[] header = new byte[15];

	private byte[] checkbyte = new byte[1];

	private byte[] sendByteBuff = new byte[65565];

	private int seq = 0;

	int readybyte = 0;

	private AppSendModel heat = new AppSendModel();
	private SendHeat sendHeatData = new SendHeat();

	public NetWorkSocket(String ipaddress) {
		serverip = ipaddress;
		this.setName("GCI-Socket");
	}

	public void initMediaData() {
		videoGciBuffs.init();
	}

	/**
	 * 发送数据线程
	 */
	private Thread sendmessagethread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true) {
				TransDataBase trans = null;
				try {
					if (send_message_queue.size() > 0) {
						synchronized (sendlock) {
							trans = send_message_queue.getFirst();
							if (trans != null) {
								send_message_queue.removeFirst();
							}
							if (trans.isNeedReSend) {
								trans.sendtime = System.currentTimeMillis();
								re_send_message_queue.add(trans);
							}
							if (trans != null)
								sendMessageModel(trans, trans.length);
						}
					} else {
						try {
							Thread.sleep(2);
						} catch (InterruptedException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					Log.e("Tag", "出错");
				}

			}
		}
	}, "GCI-SendMessageThead-0.3s");

	private Thread reSendMessageThread = new Thread(new Runnable() {
		@Override
		public void run() {
			TransDataBase trans = null;
			List<TransDataBase> lis = new ArrayList<TransDataBase>();
			while (true) {
				try {
					if (re_send_message_queue.size() > 0) {
						synchronized (sendlock) {
							while (true) {
								if (re_send_message_queue.size() == 0)
									break;
								trans = re_send_message_queue.get(0);
								if (trans != null) {
									if ((System.currentTimeMillis() - trans.sendtime) >= 5000) {
										if (trans.online_re_send_count <= 5) {
											if (!isLostLine && socket != null && socket.isConnected())
												trans.online_re_send_count++;
											send_message_queue.addFirst(trans);
										}
									} else {
										lis.add(trans);
									}
								}
								try {
									re_send_message_queue.remove(0);
								} catch (Exception e) {
								}
							}
						}

						if (lis.size() > 0) {
							re_send_message_queue.addAll(lis);
							lis.clear();
						}
					}
					Thread.sleep(2000);
				} catch (Exception e) {

				}
			}
		}
	});

	private Thread doCallBackThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true) {
				ByteBuffer messagebuff = buffs.getOutBuff();
				if (messagebuff != null) {
					try {
						String json = new String(messagebuff.getBuff(), 0, messagebuff.getDataPostion(), "GBK");
						android.util.Log.d("Gson", json);
						final ResponseBer m = gson.fromJson(json, ResponseBer.class);
						if (m != null) {
							if (re_send_message_queue.size() > 0) {
								synchronized (sendlock) {
									for (int i = 0; i < re_send_message_queue.size(); i++) {
										if (re_send_message_queue.get(i).fun.equals(m.MsgType) && re_send_message_queue.get(i).messageno.equals(m.MsgType) && re_send_message_queue.get(i).tag == m.seq) {
											re_send_message_queue.remove(i);
											Log.e("移除", "移除反馈");
										}
									}
								}
							}
							new NetWorkCallBackThread(m).start();
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					buffs.reCallBuff();
				}
				try {
					if (buffs.getPosion() == 0) {
						Thread.sleep(30);
					} else {
						Thread.sleep(2);
					}
				} catch (Exception e) {
				}
			}
		}
	});

	public class NetWorkCallBackThread extends Thread {
		private ResponseBer m;

		public NetWorkCallBackThread(ResponseBer model) {
			this.m = model;
		}

		@Override
		public void run() {
			if (true == m.isSuccess) {
				if (reslist.containsKey(m.MsgType)) {
					Hashtable<String, HashMap<Integer, OnResponseListener>> map = reslist.get(m.MsgType);
					if (map.containsKey(m.MsgType)) {
						HashMap<Integer, OnResponseListener> listermap = map.get(m.MsgType);
						try {
							if (listermap.containsKey(0)) {
								listermap.get(0).res(m.JsonString, null);
							}

							if (listermap.containsKey(m.seq)) {
								listermap.get(m.seq).res(m.JsonString, null);
								listermap.remove(m.seq);
							}
						} catch (Exception e) {

						}
					}
				}
			} else {

				if (m.ErrCode == -1001) {
					networklinstener.onNeedReLogin();
					return;
				}

				if (m.MsgType != "") {
					Hashtable<String, HashMap<Integer, OnResponseListener>> map = reslist.get(m.MsgType);
					if (map != null && null != m.MsgType && m.MsgType != "") {
						if (map.containsKey(m.MsgType)) {
							HashMap<Integer, OnResponseListener> listermap = map.get(m.MsgType);

							if (listermap.containsKey(0)) {
								listermap.get(0).onerror(m.ErrInfo, null);
								listermap.get(0).onerror(m.ErrCode, m.ErrInfo, null);
							}

							if (listermap.containsKey(m.seq)) {
								listermap.get(m.seq).onerror(m.ErrInfo, null);
								listermap.get(m.seq).onerror(m.ErrCode, m.ErrInfo, null);
								listermap.remove(m.seq);
							}
						}
					} else {
						// Log.d("平板", json);
					}
				}
			}
		}
	}

	private Thread sendHeatThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (true) {
				try {
					if (socket != null && socket.isConnected()) {
						sendHeatData.Id = GroupVarManager.getIntance().userId;
						heat.JsonString = CommTool.gson.toJson(sendHeatData);
						heat.MsgType = SystemMsgType.HEAT_BREAT;
						heat.Seq = CommTool.getWorkSegByFunctionNo(SystemMsgType.HEAT_BREAT, SystemMsgType.HEAT_BREAT);
						sendMessageNeedReCall(heat, (byte) 0x01);
						Thread.sleep(50 * 1000);
					}
				} catch (Exception e) {
					// TODO: handle exception
				}
			}
		}
	});

	/**
	 * 读取Socket内容
	 */
	@Override
	public void run() {
		while (!Thread.interrupted()) {
			InputStream inps = null;
			try {
				if (seq > 5) {
					isLostLine = true;
					networklinstener.onTimeOut();
					seq = 0;
				}
				seq++;

				if (isLostLine) {
					socket = linkserver();
				}
				inps = socket.getInputStream();
			} catch (Exception e) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException ex) {
					e.printStackTrace();
				}
				continue;
			}

			if (isLostLine) {
				isLostLine = false;
				networklinstener.onReLine();
				seq = 0;
			}

			try {
				int s = 0;
				while ((readybyte = inps.read(checkbyte, 0, 1)) != -1) {
					if (isLostLine) {
						isLostLine = false;
						networklinstener.onReLine();
					}

					if (!checkHeader()) {
						s++;
						continue;
					} else {
						Log.e("找到包头的次数", s + "");
						s = 0;
						try {
							// 读固定头
							fill(inps, header, 6, 9);
							int packlen = (int) BufferTool.bytes2Long(header, 6, 10);

							if (packlen < 65535) {
								byte type = header[14];
								if (type == 0x00) {
									RTPBuffModel mVideoBuffer = videoGciBuffs.getInBuff();
									if (mVideoBuffer != null) {
										mVideoBuffer.init();
										fill(inps, mVideoBuffer.getBuff(), 0, packlen);
										mVideoBuffer.datapostion += packlen;
										videoGciBuffs.commitBuff();
									}
								} else if (type == 0x02) {
									RTPBuffModel mVideoBuffer = audioGciBuffs.getInBuff();
									mVideoBuffer.init();
									fill(inps, mVideoBuffer.getBuff(), 0, packlen);
									mVideoBuffer.datapostion += packlen;
									audioGciBuffs.commitBuff();
								} else {
									ByteBuffer mByteBuffer = buffs.getInBuff();
									mByteBuffer.init();
									fill(inps, mByteBuffer.getBuff(), 0, packlen);
									mByteBuffer.datapostion += packlen;
									buffs.commitBuff();
								}
							}
						} catch (Exception e) {
							// e.printStackTrace();

							StringBuffer sb = new StringBuffer();
							Writer writer = new StringWriter();
							PrintWriter printWriter = new PrintWriter(writer);
							e.printStackTrace(printWriter);
							Throwable cause = e.getCause();
							while (cause != null) {
								cause.printStackTrace(printWriter);
								cause = cause.getCause();
							}
							printWriter.close();
							String result = writer.toString();
							sb.append(result);

							SendLog data = new SendLog();
							data.ErrorTitle = "崩溃性质的异常";
							data.ErrInfo = sb.toString();
							if (!NetServer.getIntance().getSocket().isLostLine) {
								AppSendModel send = NetServer.getIntance().getSendModel(SystemMsgType.MSG_LOG, data);
								NetServer.getIntance().getSocket().AddMessagePairListener(send, new OnResponseListener() {
									@Override
									public void res(String json, Object sender) {
									}
								});
							}

							networklinstener.onTimeOut();
						}

						for (int i = 0; i < header.length; i++)
							header[i] = 0x2;
					}
				}
				Thread.sleep(1000);
			} catch (Exception e) {
				android.util.Log.e("线程奔溃", "接收线程奔溃");
				networklinstener.onTimeOut();
				isLostLine = true;
				// StartServer(null);
			}
		}
	}

	private boolean checkHeader() {
		boolean result = true;
		for (int i = 0; i < 6; i++) {
			if (i != 5) {
				header[i] = header[i + 1];
				if (header[i] != 0x00)
					result = false;
			} else {
				header[i] = checkbyte[0];
				if (header[i] != (byte) 0xff)
					result = false;
			}
		}
		return result;
	}

	private int fill(InputStream is, byte[] buffer, int offset, int length) throws IOException {
		int sum = 0, len;
		while (sum < length) {
			len = is.read(buffer, offset + sum, length - sum);
			if (len < 0) {
				Log.e("len", len + "");
				throw new IOException("End of stream");
			} else
				sum += len;
		}
		return sum;
	}

	public void StartServer(OnNetWorkLinstener timeout) {
		networklinstener = timeout;
		reslist = new Hashtable<String, Hashtable<String, HashMap<Integer, OnResponseListener>>>();

		try {
			if (socket != null) {
				socket = linkserver();
				ower.start();
			} else {
				socket = linkserver();
				if (socket != null) {
					ower.start();
					sendmessagethread.start();
					reSendMessageThread.start();
					doCallBackThread.start();
					sendHeatThread.start();
					isFistStart = true;
					networklinstener.onLinkSuccess();
				} else {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					StartServer(timeout);
				}
			}
		} catch (UnknownHostException e) {
			if (networklinstener != null)
				networklinstener.onTimeOut();
		} catch (IOException e) {
			if (networklinstener != null)
				networklinstener.onTimeOut();
		}

		/*
		 * Thread t = new Thread(new Runnable() {
		 * 
		 * @Override public void run() { while (true) { try { socket =
		 * linkserver(); if (socket != null) { ower.start(); break; } } catch
		 * (UnknownHostException e) { if (timeoutlinter != null)
		 * timeoutlinter.onTimeOut(); } catch (IOException e) { if
		 * (timeoutlinter != null) timeoutlinter.onTimeOut(); } } //
		 * this.finalize() } }); t.start();
		 */
	}

	/**
	 * 设置 Socket接收 监听�?
	 * 
	 * @param funcitonno
	 *            功能编号
	 * @param MessegeNo
	 *            消息编号
	 * @param WorkSeg
	 *            作业序号，如果为0，则全部回调
	 * @param listener
	 */
	@SuppressLint("UseSparseArrays")
	public void AddListener(String funcitonno, String MessegeNo, int WorkSeg, OnResponseListener listener) {
		boolean isHas = reslist.containsKey(funcitonno);
		if (!isHas) {
			reslist.put(funcitonno, new Hashtable<String, HashMap<Integer, OnResponseListener>>());
		}

		Hashtable<String, HashMap<Integer, OnResponseListener>> hash = reslist.get(funcitonno);

		if (!hash.containsKey((MessegeNo))) {
			hash.put(MessegeNo, new HashMap<Integer, OnResponseListener>());
		}

		HashMap<Integer, OnResponseListener> listermap = hash.get(MessegeNo);

		listermap.put(WorkSeg, listener);
	}

	/**
	 * 添加问答消息�?
	 * 
	 * @param message
	 *            发�?�的数据对象
	 * @param listener
	 *            服务器返回后的回�?
	 */
	public void AddMessagePairListener(AppSendModel message, final OnResponseListener listener) {
		if (true == sendMessageNeedReCall(message, (byte) 0x01)) {
			if (listener != null) {
				AddListener(message.MsgType, message.MsgType, message.Seq, listener);
			}
		}
	}

	public boolean sendMessageNeedReCall(AppSendModel message, byte type) {
		String json = gson.toJson(message);
		TransDataBase base = new TransDataBase();
		base.fun = message.MsgType;
		base.messageno = message.MsgType;
		base.tag = message.Seq;
		base.isNeedReSend = true;

		try {
			base.sendBytes = json.getBytes("GBK");
			base.length = base.sendBytes.length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		base.type = type;
		return sendMessage(base);
	}

	public boolean sendMessage(AppSendModel message, byte type) {
		String json = gson.toJson(message);
		TransDataBase base = new TransDataBase();
		base.fun = message.MsgType;
		base.messageno = message.MsgType;
		base.tag = message.Seq;
		base.isNeedReSend = false;

		try {
			base.sendBytes = json.getBytes("GBK");
			base.length = base.sendBytes.length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		base.type = type;
		return sendMessage(base);
	}

	public boolean sendMediaPack(byte[] buffers, int length) {
		RTPBuffModel bys = sendVideoGcBuffs.getInBuff();
		if (bys != null) {
			bys.init();
			bys.write(buffers, 0, length);
			TransDataBase base = new TransDataBase();
			base.type = 0x00;
			base.sendBytes = bys.getBuff();
			base.length = bys.getDataPostion();
			sendVideoGcBuffs.commitBuff();
			return sendMessage(base);
		}
		return false;
	}

	public boolean sendAudioMideaPack(byte[] buffers, int length) {
		RTPBuffModel bys = sendAudoGciBuffs.getInBuff();
		if (bys != null) {
			bys.init();
			bys.write(buffers, 0, length);
			TransDataBase base = new TransDataBase();
			base.type = 0x02;
			base.sendBytes = bys.getBuff();
			base.length = bys.getDataPostion();
			sendAudoGciBuffs.commitBuff();
			return sendMessage(base);
		}
		return false;
	}

	@SuppressWarnings("unused")
	private void sendMessageModel(TransDataBase base, int length) {
		try {
			char[] usercs = GroupVarManager.getIntance().getUserIdChars();

			sendByteBuff[0] = 0x00;
			sendByteBuff[1] = 0x00;
			sendByteBuff[2] = 0x00;
			sendByteBuff[3] = 0x00;
			sendByteBuff[4] = 0x00;
			sendByteBuff[5] = (byte) 0xff;

			BufferTool.setLong(sendByteBuff, base.length, 6, 10);
			sendByteBuff[10] = (byte) usercs[0];
			sendByteBuff[11] = (byte) usercs[1];
			sendByteBuff[12] = (byte) usercs[2];
			sendByteBuff[13] = (byte) usercs[3];
			sendByteBuff[14] = base.type;

			System.arraycopy(base.sendBytes, 0, sendByteBuff, 15, base.length);

			switch (base.type) {
			case 0:
				sendVideoGcBuffs.getOutBuff();
				break;
			case 1:
				break;
			case 2:
				sendAudoGciBuffs.getOutBuff();
				break;
			default:
				break;
			}
			socket.getOutputStream().write(sendByteBuff, 0, base.length + 15);
			socket.getOutputStream().flush();

			base = null;

			if (isLostLine) {
				networklinstener.onReLine();
				isLostLine = false;
			}

		} catch (UnsupportedEncodingException e) {
			Log.v("Socket", "发送数据失败！");
			if (!isLostLine) {
				networklinstener.onTimeOut();
				isLostLine = true;
			}
		} catch (IOException e) {
			Log.v("Socket", "发送数据失败！IO");
			if (!isLostLine) {
				networklinstener.onTimeOut();
				isLostLine = true;
			}
		}
	}

	public boolean sendMessage(TransDataBase base) {
		boolean result = false;
		synchronized (sendlock) {
			send_message_queue.offer(base);
		}
		result = true;
		return result;
	}

	private Socket linkserver() throws UnknownHostException, IOException {
		socket = new Socket(serverip, point);
		return socket;
	}
}
