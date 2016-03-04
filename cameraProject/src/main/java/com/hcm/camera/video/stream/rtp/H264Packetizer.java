/*
 * Copyright (C) 2011-2014 GUIGUI Simon, fyhertz@gmail.com
 * 
 * This file is part of libstreaming (https://github.com/fyhertz/libstreaming)
 * 
 * Spydroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.hcm.camera.video.stream.rtp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.nio.ByteBuffer;

import com.gci.nutil.ShellUtils.CommandResult;
import com.hcm.camera.comm.CommTool;
import com.hcm.camera.data.GroupVarManager;
import com.hcm.camera.data.RTPBuffModel;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.video.SendVideoConfig;
import com.hcm.camera.net.model.video.VideoMsgType;
import com.hcm.camera.video.VideoQuality;

import android.R.style;
import android.annotation.SuppressLint;
import android.provider.ContactsContract.Contacts.Data;
import android.util.Log;
import android.view.View;

/**
 * 
 * RFC 3984.
 * 
 * H.264 streaming over RTP.
 * 
 * ������ιInputStream H.264 NAL��Ԫ����֮ǰ�ĳ��ȣ�4�ֽڣ�.
 * ��������MPEG4��3GPP��ͷ�����ᱻ������
 * 
 */
public class H264Packetizer extends AbstractPacketizer implements Runnable {

	public final static String TAG = "H264Packetizer";

	private Thread t = null;
	private int naluLength = 0;
	private long delay = 0, oldtime = 0;
	private Statistics stats = new Statistics();
	private byte[] sps = null, pps = null;
	byte[] header = new byte[5];
	private int count = 0;
	private int streamType = 1;

	public H264Packetizer() {
		super();
		socket.setClockFrequency(90000);
	}

	public void start() {
		if (t == null) {
			t = new Thread(this);
			t.start();
		}
	}

	public void stop() {
		Log.e("����", "ֹͣ");
		if (t != null) {
			try {
				is.close();
			} catch (IOException e) {
			}
			t.interrupt();
			t = null;
		}
	}

	public void setStreamParameters(byte[] pps, byte[] sps) {
		this.pps = pps;
		this.sps = sps;
	}

	public void run() {
		long duration = 0, delta2 = 0;
		Log.d(TAG, "H264 packetizer started !");
		stats.reset();
		count = 0;

		SendVideoConfig config = new SendVideoConfig();
		config.Width = VideoQuality.DEFAULT_VIDEO_QUALITY.resX;
		config.Height = VideoQuality.DEFAULT_VIDEO_QUALITY.resY;
		config.TaskId = GroupVarManager.getIntance().loginUserInfo.Id;

		if (is instanceof MediaCodecInputStream) {
			streamType = 1;
			socket.setCacheSize(0);
		} else if (myJpegBuffer != null) {
			streamType = 4;
			socket.setCacheSize(25);
			config.EcoderType = 1;
		} else if (myBuff != null) {
			streamType = 2;
			socket.setCacheSize(0);
			config.EcoderType = 0;
		} else if (myEncoderBuffer != null) {
			streamType = 3;
			socket.setCacheSize(0);
		} else {
			streamType = 0;
			socket.setCacheSize(0);
		}

		AppSendModel send = new AppSendModel();
		send.JsonString = CommTool.gson.toJson(config);
		send.MsgType = VideoMsgType.MSG_VIDEO_CONFIG;
		send.Seq = CommTool.getWorkSegByFunctionNo(VideoMsgType.MSG_VIDEO_CONFIG, VideoMsgType.MSG_VIDEO_CONFIG);
		NetServer.getIntance().getSocket().sendMessage(send, (byte) 0x01);

		boolean isFrist = true;
		int count = 0;
		while (!Thread.interrupted()) {
			try {
				oldtime = System.nanoTime();
				// ���ǿ�һ��NAL��Ԫ��������������������
				send();
				// ���ǲ����˶೤ʱ����ֻ�����NAL��Ԫ
				duration = System.nanoTime() - oldtime;

				// ÿ3�룬���ǰ�����NALU 7�ͣ�SPS����8��PPS��
				// ��ЩӦ����H264�������н��룬��ʹû��SDP�����͵���������
				if (streamType != 4) {
					delta2 += duration / 1000000;
					if ((delta2 > 3000 && streamType == 0) || (isFrist && count < 4 && delta2 > 3000)) {
						count++;
						delta2 = 0;
						if (sps != null) {
							buffer = socket.requestBuffer();
							socket.markNextPacket();
							socket.updateTimestamp(ts);
							System.arraycopy(sps, 0, buffer, rtphl, sps.length);
							super.send(rtphl + sps.length);
						}
						if (pps != null) {
							buffer = socket.requestBuffer();
							socket.updateTimestamp(ts);
							socket.markNextPacket();
							System.arraycopy(pps, 0, buffer, rtphl, pps.length);
							Log.e("SPS", "����PPS");
							super.send(rtphl + pps.length);
						}
					}
				}

				stats.push(duration);
				// ����һ��NAL��Ԫ��ƽ��ʱ��
				delay = stats.average();
				// Log.d(TAG,"duration: "+duration/1000000+" delay: "+delay/1000000);
				Thread.sleep(2);
			} catch (IOException e) {
				e.printStackTrace();
				// Log.e("Tag", e.getMessage());
			} catch (InterruptedException e) {
				e.printStackTrace();
				// Log.e("Tag", e.getMessage());
			}
		}

		// Log.d(TAG, "H264 packetizer stopped !");

	}

	// private byte[] outData = new byte[bufferInfo.size];
	// private byte[] inputbyte = new byte[]
	/**
	 * ��ȡFIFO��NAL��Ԫ���͡� �����̫���ˣ����ǰ���fu-a��λ��RFC 3984����
	 */
	@SuppressLint("NewApi")
	private void send() throws IOException, InterruptedException {
		int sum = 1, len = 0, type;
		// ����
		if (streamType == 0) {
			// NAL��Ԫ���������ǵĳ��ȣ����ǽ����ĳ���
			fill(header, 0, 5);
			ts += delay;
			naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8 | (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
			if (naluLength > 100000 || naluLength < 0)
				resync();
			// ����
		} else if (streamType == 4) {
			com.hcm.camera.data.net.ByteBuffer m = myJpegBuffer.getOutBuff();
			if (m != null) {
				is = m;
				header[4] = 0x2f;
				naluLength = m.getDataPostion() + 1;
				Log.e("Jpeg", naluLength + "");
				if (naluLength == 0)
					return;
			} else {
				return;
			}
		} else if (streamType == 2) {
			RTPBuffModel m = myBuff.getOutBuff();
			if (m != null) {
				is = m;
				fill(header, 0, 1);
				header[4] = header[0];
				naluLength = m.getDataPostion() + 1;
			} else {
				return;
			}
		} else if (streamType == 3) {
			com.hcm.camera.data.net.ByteBuffer m = myEncoderBuffer.getOutBuff();
			if (m != null) {
				is = m;
				fill(header, 0, 1);
				header[4] = header[0];
				naluLength = m.getDataPostion() + 1;
				Log.e("编码", naluLength + "");
			} else {
				return;
			}
		} else if (streamType == 1) {
			Log.e("Tag", "laji");
			// NAL��Ԫ�ǽ�����0x00000001
			fill(header, 0, 5);
			ts = ((MediaCodecInputStream) is).getLastBufferInfo().presentationTimeUs * 1000L;
			ts += delay;
			naluLength = is.available() + 1;
			if (!(header[0] == 0 && header[1] == 0 && header[2] == 0)) {
				// ԭ����NAL��Ԫ��������0x00000001
				Log.e(TAG, "NAL units are not preceeded by 0x00000001");
				streamType = 2;
				return;
			}
			// fill(header, 0, 1);
			// header[4] = header[0];

		} else {
			// Nothing preceededs the NAL units
			// ��ʹ��Nal��Ԫ
			fill(header, 0, 1);
			header[4] = header[0];
			ts = ((MediaCodecInputStream) is).getLastBufferInfo().presentationTimeUs * 1000L;
			// ts += delay;
			naluLength = is.available() + 1;
		}

		// Parses the NAL unit type
		type = header[4] & 0x1F;

		// The stream already contains NAL unit type 7 or 8, we don't need
		// to add them to the stream ourselves
		if (type == 7 || type == 8) {
			Log.v(TAG, "SPS or PPS present in the stream.");
			count++;
			if (count > 4) {
				sps = null;
				pps = null;
			}
		}

		// Log.d(TAG,"- Nal unit length: " + naluLength +
		// " delay: "+delay/1000000+" type: "+type);

		// Small NAL unit => Single NAL unit
		if (naluLength <= MAXPACKETSIZE - rtphl - 2) {
			buffer = socket.requestBuffer();
			buffer[rtphl] = header[4];
			len = fill(buffer, rtphl + 1, naluLength - 1);
			socket.updateTimestamp(ts);
			socket.markNextPacket();
			if (streamType == 2) {
				((RTPBuffModel) is).init();
			} else if (streamType == 3) {
				myEncoderBuffer.reCallBuff();
			} else if (streamType == 4) {
				myJpegBuffer.reCallBuff();
			}
			super.send(naluLength + rtphl);
			// Log.d(TAG,"----- Single NAL unit - len:"+len+" delay: "+delay);
		}
		// Large NAL unit => Split nal unit
		else {

			// Set FU-A header
			header[1] = (byte) (header[4] & 0x1F); // FU header type
			header[1] += 0x80; // Start bit
			// Set FU-A indicator
			header[0] = (byte) ((header[4] & 0x60) & 0xFF); // FU indicator NRI
			header[0] += 28;

			while (sum < naluLength) {
				buffer = socket.requestBuffer();
				buffer[rtphl] = header[0];
				buffer[rtphl + 1] = header[1];
				socket.updateTimestamp(ts);
				if ((len = fill(buffer, rtphl + 2, naluLength - sum > MAXPACKETSIZE - rtphl - 2 ? MAXPACKETSIZE - rtphl - 2 : naluLength - sum)) < 0)
					return;
				sum += len;
				// Last packet before next NAL
				if (sum >= naluLength) {
					// End bit on
					buffer[rtphl + 1] += 0x40;
					socket.markNextPacket();
				}
				if (streamType == 2) {
					((RTPBuffModel) is).init();
				} else if (streamType == 3) {
					myEncoderBuffer.reCallBuff();
				} else if (streamType == 4) {
					myJpegBuffer.reCallBuff();
				}
				super.send(len + rtphl + 2);
				// Switch start bit
				header[1] = (byte) (header[1] & 0x7F);
				// Log.d(TAG,"----- FU-A unit, sum:"+sum);
			}
		}
	}

	private int fill(byte[] buffer, int offset, int length) throws IOException {
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

	private void resync() throws IOException {
		int type;

		Log.e(TAG, "Packetizer out of sync ! Let's try to fix that...(NAL length: " + naluLength + ")");

		while (true) {

			header[0] = header[1];
			header[1] = header[2];
			header[2] = header[3];
			header[3] = header[4];
			header[4] = (byte) is.read();

			type = header[4] & 0x1F;

			if (type == 5 || type == 1) {
				naluLength = header[3] & 0xFF | (header[2] & 0xFF) << 8 | (header[1] & 0xFF) << 16 | (header[0] & 0xFF) << 24;
				if (naluLength > 0 && naluLength < 100000) {
					oldtime = System.nanoTime();
					Log.e(TAG, "A NAL unit may have been found in the bit stream !");
					break;
				}
				if (naluLength == 0) {
					Log.e(TAG, "NAL unit with NULL size found...");
				} else if (header[3] == 0xFF && header[2] == 0xFF && header[1] == 0xFF && header[0] == 0xFF) {
					Log.e(TAG, "NAL unit with 0xFFFFFFFF size found...");
				}
			}

		}

	}

}