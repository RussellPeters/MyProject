package com.hcm.camera.bluetooth;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.UUID;

import com.gci.nutil.activity.GciActivityManager;
import com.gci.nutil.base.callbackinterface.OnMessageBoxClickListener;
import com.gci.nutil.dialog.GciDialogManager;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnNetWorkLinstener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.system.ResponseBluetoohCmd;
import com.hcm.camera.net.model.system.SystemMsgType;

import android.R.bool;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

public class BluetoothDriverManager {
	int seq = 0;
	private static BluetoothDriverManager _b = null;
	private final static String MY_UUID = "00001101-0000-1000-8000-00805F9B34FB"; // SPP服务UUID号
	private boolean isSendServer = false;
	private String userId = "";

	public void setSendServer(boolean blg) {
		this.isSendServer = blg;
	}

	public void setUserId(String userID) {
		this.userId = userID;
	}

	/**
	 * 蓝牙网络监听器
	 */
	private OnNetWorkLinstener networklinstener = new OnNetWorkLinstener() {
		/**
		 * 蓝牙通讯断开时回调
		 */
		@Override
		public void onTimeOut() {
			GciDialogManager.getInstance().showTextToast("蓝牙连接已断开",
					GciActivityManager.getInstance().getLastActivity());
		}

		/**
		 * 蓝牙通讯重新连接后回调
		 */
		@Override
		public void onReLine() {
			// TODO Auto-generated method stub
		}

		@Override
		public void onNeedReLogin() {
			// TODO Auto-generated method stub

		}

		@Override
		public void onLinkSuccess() {
			// TODO Auto-generated method stub

		}
	};

	/** 待发送数据 队列 */
	private LinkedList<byte[]> _send_queue = new LinkedList<byte[]>();

	/** 缓存 */
	private byte[] bffer = new byte[2];

	public static BluetoothDriverManager getIntance() {
		if (_b == null)
			_b = new BluetoothDriverManager();
		return _b;
	}

	private String _macAddress = "";
	BluetoothDevice _device = null; // 蓝牙设备
	BluetoothSocket _socket = null; // 蓝牙通信socket
	boolean _discoveryFinished = false;
	boolean bRun = true; // 当前所有线程是否是运行状态
	private Object lock = new Object();
	private boolean isLostLine = false; // 当前蓝牙通讯是否是断开状态
	private boolean isStart = false; // 当前单利是否已经是开始状态

	private onBluetoothHandler handler = null;
	private BluetoothAdapter _bluetooth = BluetoothAdapter.getDefaultAdapter();

	public void start(String address) {
		if (null == address || "".equals(address)) {
			GciDialogManager.getInstance()
					.showMessageBox("提示", "请先到蓝牙设置中配对设备，并在登录时选择配对设备！", false,
							new OnMessageBoxClickListener() {

								@Override
								public void onClickOK() {
									// Intent intent = new Intent(
									// Settings.ACTION_DATA_ROAMING_SETTINGS);
									// GciActivityManager.getInstance()
									// .getLastActivity()
									// .startActivity(intent);
									// GciActivityManager.getInstance().finishAll(
									// null);
								}
							},
							GciActivityManager.getInstance().getLastActivity(),
							null);
			return;
		}

		if (!_bluetooth.isEnabled()) {
			// GciDialogManager.getInstance().showTextToast("请先打开蓝牙设备！",GciActivityManager.getInstance().getLastActivity());

			GciDialogManager.getInstance()
					.showMessageBox(
							"提示",
							"请先打开蓝牙设备并重新登录下位机",
							false,
							new OnMessageBoxClickListener() {

								@Override
								public void onClickOK() {
									Intent intent = new Intent(
											Settings.ACTION_DATA_ROAMING_SETTINGS);
									GciActivityManager.getInstance()
											.getLastActivity()
											.startActivity(intent);
									GciActivityManager.getInstance().finishAll(
											null);
								}
							},
							GciActivityManager.getInstance().getLastActivity(),
							null);
			return;
		}
		setHandlerCallBacl();
		// 保存连接设备的Mac地址
		this._macAddress = address;
		// 获取设备的驱动信息
		_device = _bluetooth.getRemoteDevice(address);
		// 开启接收线程
		if (!recThread.isAlive())
			recThread.start();
		// 开启发送线程
		if (!sendThread.isAlive())
			sendThread.start();

//		if (isStart) {
//			try {
//				// 如果当前已经处理连接状态了，则不需要关闭线程，只需要对蓝牙的Socket进行重置。
//				LinkBlue();
//			} catch (IOException e) {
//				_socket = null;
//			}
//		} else {
//			isStart = true;
//		}
	}

	/**
	 * 连接蓝牙设备
	 * 
	 * @throws IOException
	 */
	@SuppressLint("NewApi")
	private void LinkBlue() throws IOException {
		if (_socket == null || !_socket.isConnected()) {
			_socket = _device.createRfcommSocketToServiceRecord(UUID
					.fromString(MY_UUID));
			_socket.connect();
			GciDialogManager.getInstance().showTextToast("蓝牙设备已连接",
					GciActivityManager.getInstance().getLastActivity());
		}
	}

	Thread recThread = new Thread(new Runnable() {
		private void resetMemey(byte[] by) {
			for (int i = 0; i < by.length; i++) {
				by[i] = (byte) 0xff;
			}
		}

		/**
		 * 从输出流中读取指定长度的数据
		 * 
		 * @param is
		 * @param buffer
		 *            缓存
		 * @param offset
		 *            缓存的 开始位置
		 * @param length
		 *            长度
		 * @return
		 * @throws IOException
		 */
		private int fill(InputStream is, byte[] buffer, int offset, int length)
				throws IOException {
			int sum = 0, len;
			while (sum < length) {
				len = is.read(buffer, offset + sum, length - sum);
				if (len < 0) {
					throw new IOException("End of stream");
				} else
					sum += len;
			}
			return sum;
		}

		/**
		 * 找数据包头
		 * 
		 * @param inpus
		 * @return
		 * @throws IOException
		 */
		private int checkHead(InputStream inpus) throws IOException {
			int len = -1;
			inpus.read(bffer, 0, 2);
			if (bffer[0] >= 0 && bffer[0] <= 13) {
				byte b = bffer[1];
				if (b < 0) {
					len = (b + 256);
				}
				if (len > 200) {
					len = -1;
				} else {
					len = b;
				}
			}

			if (len == -1)
				bffer[0] = bffer[1];
			return len;
		}

		@Override
		public void run() {
			InputStream is = null;
			while (bRun) {
				try {
					try {
						LinkBlue();
						is = _socket.getInputStream();
					} catch (Exception e) {
						seq++;
						if (seq >= 6) {
							if (networklinstener != null) {
								networklinstener.onTimeOut();
								isLostLine = true;
								seq = 0;
							}
						}
						continue;
					}

					isLostLine = false;
					while (bRun) {
						int len = checkHead(is);
						if (len != -1) {
							byte[] bytes = new byte[len + 2];
							System.arraycopy(bffer, 0, bytes, 0, 2);
							resetMemey(bffer);
							fill(is, bytes, 2, len);
							new HandlerThread(bytes).start();
						}
					}
					_socket.close();
				} catch (Exception e) {
					_socket = null;
					e.printStackTrace();
				}
			}
		}
	});

	Thread sendThread = new Thread(new Runnable() {
		@Override
		public void run() {
			while (bRun) {
				try {
//					try {
//						if (_socket == null && !isLostLine) {
//							isLostLine = true;
//							LinkBlue();
//						}
//					} catch (Exception e) {
//						seq++;
//						if (seq >= 6) {
//							if (networklinstener != null) {
//								networklinstener.onTimeOut();
//								isLostLine = true;
//								seq = 0;
//							}
//						}
//						Thread.sleep(1000);
//						continue;
//					}
					if (_send_queue.size() > 0 && !isLostLine) {
						synchronized (lock) {
							for (int i = 0; i < _send_queue.size(); i++) {
								_socket.getOutputStream().write(
										_send_queue.get(i));
								_socket.getOutputStream().flush();
								Log.e("Blue", "发送数据"
										+ _send_queue.get(i).length);
							}
							_send_queue.clear();
						}
					} else {
						Thread.sleep(300);
					}
				} catch (Exception e) {

				}
			}
		}
	});

	/**
	 * 设置蓝牙监听处理
	 */
	onBluetoothHandler locationBlue = new onBluetoothHandler() {
		private int getIntByByte(byte b) {
			int result = b;
			if (b < 0)
				result = (b + 256);
			return result;
		}

		@Override
		public void onHandler(byte[] bytes) {
			// boolean isSend = false;
			String msg = "";
			switch (bytes[0]) {
			case 0:
				msg = "设备成功收到命令";
				// isSend = true;
				break;
			case 1:
				if (bytes[1] == 6) {
					switch (bytes[2]) {
					case 1:
						String temp1 = "湿度:" + getIntByByte(bytes[3]) + "."
								+ String.format("%03d", getIntByByte(bytes[4]));
						String temp2 = "温度:" + getIntByByte(bytes[5]) + "."
								+ String.format("%03d", getIntByByte(bytes[6]));
						msg = temp1 + "," + temp2;
						break;
					case 2:
						msg = "温湿度传感器信号超时";
						break;
					default:
						msg = " 温湿度传感器,未知错误";
						break;
					}
				} else {
					msg = "数据长度有误,命令代号:1";
				}
				// isSend = true;
				break;
			case 2:
				switch (bytes[2]) {
				case 1:
					msg = "有收到数据，数据长度为:" + getIntByByte(bytes[3])
							+ "(数据改如何显示?)";
					// ;
					break;
				case 2:
					msg = "未接收到任何信号";
					break;
				default:
					msg = " 指针溢出错误";
					break;
				}
				// isSend = true;
				break;
			case 3:
				if (bytes[1] == 1) {
					if (bytes[2] == 0)
						msg = "未检测到遮挡，正常接收信号";
					else
						msg = "检测到遮挡，失去信号";
				} else {
					msg = "红外遮挡传感器反馈异常";
				}
				// isSend = true;
				break;
			case 4:
				msg = "暂未定义";
				// isSend = true;
				break;
			case 5:
				msg = "红外遥控器发射报告";
				// isSend = true;
				break;
			case 6:
				msg = "红外遮挡传感器发射报告";
				// isSend = true;
				break;
			case 7:
				if (bytes[1] == 10) {
					// int pm = (int) BufferTool.bytes2Long(bytes, 2, 4);
					// int mq = (int) BufferTool.bytes2Long(bytes, 4, 6);
					// int mp4 = (int) BufferTool.bytes2Long(bytes, 6, 8);
					// int mp503 = (int) BufferTool.bytes2Long(bytes, 8, 10);
					// int me = (int) BufferTool.bytes2Long(bytes, 10, 12);

					// int pm = (((int)bytes[2])&0xff +
					// (((int)bytes[3])&0xff)<<8)&0xffff;
					int pm = (bytes[2] & 0xff | (bytes[3] << 8) & 0xff00) & 0xffff;
					int mq = (bytes[4] & 0xff | (bytes[5] << 8) & 0xff00) & 0xffff;
					int mp4 = (bytes[6] & 0xff | (bytes[7] << 8) & 0xff00) & 0xffff;
					int mp503 = (bytes[8] & 0xff | (bytes[9] << 8) & 0xff00) & 0xffff;
					int me = (bytes[10] & 0xff | (bytes[11] << 8) & 0xff00) & 0xffff;

					if (pm == 65535)
						msg += "PM2.5传感器未连接 \n";
					else
						msg += "PM2.5传感器:" + pm + " ug/m3, \n";

					if (mq == 65535)
						msg += "MQ7一氧化碳传感器未连接 \n";
					else
						msg += "MQ7一氧化碳传感器:" + mq + " ppm, \n";

					if (mp4 == 65535)
						msg += "MP4可燃气体传感器未连接 \n";
					else
						msg += "MP4可燃气体传感器:" + mp4 + " ug/m3, \n";

					if (mp503 == 65535)
						msg += "MP503空气质量传感器未连接 \n";
					else
						msg += "MP503空气质量传感器:" + mp503 + " ppm, \n";

					// if( me == 65535 )
					// msg += "ME2CO一氧化碳传感器未连接 \n";
					// else
					// msg += "ME2CO一氧化碳传感器:" + me + " ppm, \n";

					// msg += "ME2CO一氧化碳传感器:" + me + " ppm, \n";
				} else {
					msg = "传感器数据错误";
				}
				// isSend = true;
				break;
			case 127:
				msg = "错误指令";
				// isSend = true;
				break;
			default:
				msg = "Message Default";

				break;
			}

			BluetoothMessageManager.getInstance().addMessage(msg);

			if (isSendServer) { // isSend &&
				ResponseBluetoohCmd obj = new ResponseBluetoohCmd();
				obj.ReceiveId = userId;
				obj.SendId = GroupVarManager.getIntance().userId;
				obj.cmd = msg;
				AppSendModel send = NetServer.getIntance().getSendModel(
						SystemMsgType.MSG_RESPONSE_CMD, obj);
				NetServer.getIntance().getSocket()
						.sendMessage(send, (byte) 0x01);
			}
		}
	};

	public void sendMessage(byte[] bytes) {
		_send_queue.add(bytes);
	}

	public void setHandlerCallBacl() {
		this.handler = locationBlue;
		// this.handler = handler;
	}

	public void removeCallBack() {
		this.handler = null;
	}

	public interface onBluetoothHandler {
		public void onHandler(byte[] bytes);
	}

	public class HandlerThread extends Thread {
		private byte[] bytes = null;

		public HandlerThread(byte[] bytes) {
			this.bytes = bytes;
		}

		@Override
		public void run() {
			if (handler != null) {
				handler.onHandler(bytes);
			}
		}
	}
}
