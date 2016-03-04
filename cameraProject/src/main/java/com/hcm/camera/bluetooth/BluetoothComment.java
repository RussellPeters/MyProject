package com.hcm.camera.bluetooth;

import java.util.ArrayList;
import java.util.List;

import com.gci.nutil.Base64;
import com.gci.nutil.activity.GciActivityManager;
import com.gci.nutil.base.BaseActivity;
import com.gci.nutil.base.callbackinterface.OnListViewSelectListener;
import com.gci.nutil.dialog.GciDialogManager;
import com.hcm.camera.data.net.NetServer;
import com.hcm.camera.data.net.OnResponseListener;
import com.hcm.camera.net.model.AppSendModel;
import com.hcm.camera.net.model.system.SendBluetoohCmd;
import com.hcm.camera.net.model.system.SystemMsgType;

public class BluetoothComment {
	private static BluetoothComment _b = null;
	private List<String> _lst = new ArrayList<String>();

	public static BluetoothComment getIntance() {
		if (_b == null)
			_b = new BluetoothComment();
		return _b;
	}

	/** 初始化，并填装列表选着项 */
	private BluetoothComment() {
		_lst.add("停止所有工作"); // 0
		_lst.add("温湿度读一次"); // 1
		_lst.add("温湿度周期性读(5s)"); // 2
		_lst.add("红外遥控器等待接收"); // 3
		_lst.add("红外遥控器发射指定信号"); // 4
		_lst.add("红外遮挡传感器接收机"); // 5
		_lst.add("红外遮挡传感器发射机"); // 6
		_lst.add("步进电机"); // 7
		_lst.add("停止温湿度传感器周期性读"); // 8
		_lst.add("停止红外遮挡传感器接收机"); // 9
		_lst.add("停止红外遮挡传感器发射机"); // 10
		_lst.add("上报当前气体传感器数据"); // 11
		_lst.add("气体传感器设置指令"); // 12
		_lst.add("步进电机顺时针旋转"); // 13 步进电机旋转
		_lst.add("步进电机逆时针旋转"); // 14
	}

	/**
	 * 通过被选择项索引 对需要发送给蓝牙服务器的数据进行处理并返回
	 * 
	 * @param lstPosition
	 *            用户选择的项次
	 * @return 将要发送的数据
	 */
	private byte[] sendCmdBytesBySelectDialogPostion(int lstPosition) {
		byte[] notBuff;// = new byte[2];
		switch (lstPosition) {
		case 0: // 停止所有工作
		case 1: // 温湿度读一次
		case 3:// 红外遥控器等待接收
		case 5:// 红外遮挡传感器接收机
		case 6:// 红外遮挡传感器发射机
		case 8:// 停止温湿度传感器周期性读
		case 9:// 停止红外遮挡传感器接收机
		case 10:// 停止红外遮挡传感器发射机
		case 11:// 上报当前气体传感器数据
			// 根据协议，可直接将选择的索引号转换成Byte发送给蓝牙服务器
			notBuff = new byte[2];
			notBuff[0] = (byte) lstPosition;
			notBuff[1] = (byte) 0;
			break;
		case 2: // 温湿度周期性读
			notBuff = new byte[3];
			notBuff[0] = (byte) lstPosition;
			notBuff[1] = (byte) 1;
			notBuff[2] = (byte) 5;
			break;
		case 4:// 红外遥控器发射指定信号
			notBuff = new byte[2];
			// 快速显示一个TestToast
			// GciActivityManager.getInstance().getLastActivity()
			// 获取当前APP最后一个显示的Activity对象
			GciDialogManager.getInstance().showTextToast("暂时不可用", GciActivityManager.getInstance().getLastActivity());
			break;
		case 7:// 步进电机相关
			notBuff = new byte[2];
			GciDialogManager.getInstance().showTextToast("暂时不可用", GciActivityManager.getInstance().getLastActivity());
			break;
		case 12:// 气体传感器设置指令
			notBuff = new byte[2];
			break;
		case 13: // 步进电机顺时针旋转
			notBuff = new byte[5];
			notBuff[0] = (byte) 7;
			notBuff[1] = (byte) 3;
			notBuff[2] = (byte) 4;
			notBuff[3] = (byte) 128; // 128 为2圈
			notBuff[4] = (byte) 0;
			break;
		case 14: // 步进电机顺时针旋转
			notBuff = new byte[5];
			notBuff[0] = (byte) 7;
			notBuff[1] = (byte) 3;
			notBuff[2] = (byte) 3;
			notBuff[3] = (byte) 128; // 128 为2圈
			notBuff[4] = (byte) 0;
			break;
		default:
			notBuff = new byte[2];
			break;
		}
		return notBuff;
	}

	/**
	 * 弹出命令选择消息框（下位机）
	 * 
	 * @param base
	 */
	public void showCmd(BaseActivity base) {
		// 弹出一个选择列表 _lst为数据源
		GciDialogManager.getInstance().showSelectDialog(base, "选择发送命令", _lst, new OnListViewSelectListener<String>() {
			/**
			 * Bingobj 为选择的文本 lstPostion 为选择的索引
			 */
			@Override
			public void onSelected(String bindobj, int lstPosition) {
				byte[] notBuff = sendCmdBytesBySelectDialogPostion(lstPosition);
				// 发送蓝牙数据
				BluetoothDriverManager.getIntance().sendMessage(notBuff);
				BluetoothMessageManager.getInstance().addMessage(" 发送命令-> " + _lst.get(lstPosition));

			}
		});
	}

	/**
	 * 弹出命令选择消息框（上位机）
	 * 
	 * @param base
	 */
	public void showCmd(BaseActivity base, final String Recid, final String SendID) {
		GciDialogManager.getInstance().showSelectDialog(base, "选择发送命令", _lst, new OnListViewSelectListener<String>() {
			@Override
			public void onSelected(String bindobj, final int lstPosition) {
				boolean isSend = true;
				switch (lstPosition) {
				case 4:// 红外遥控器发射指定信号
				case 7:// 步进电机相关
				case 12:// 气体传感器设置指令
					isSend = false;
					break;
				default:
					isSend = false;
					break;
				}
				byte[] notBuff = sendCmdBytesBySelectDialogPostion(lstPosition);
				/*
				 * 给通讯服务器发送蓝牙命令数据，服务器将记录本次行为，并发下给下位机，下位机会执行给蓝牙服务器发送对应的命令
				 * SendBluetoohCmd 发送蓝牙命令的所需要的 数据模型
				 */

				SendBluetoohCmd data = new SendBluetoohCmd();
				// 接收者ID 这里指下位机ID
				data.ReceiveId = Recid;
				// 发送者ID 这里指上位机ID也就是自己的ID号
				data.SendId = SendID;
				// 将命令数据转换成Base64的文本，方便Json传递数据
				data.cmd = Base64.encode(notBuff);
				// 通用数据发送模型，会对发送数据进行协议封装（跟通讯服务器间）
				// SystemMsgType.MSG_BLUE 本次发送的消息类别（跟通讯服务器间）
				AppSendModel send = NetServer.getIntance().getSendModel(SystemMsgType.MSG_BLUE, data);
				// 发送数据并等待服务器反馈（异步回调）
				NetServer.getIntance().getSocket().AddMessagePairListener(send, new OnResponseListener() {
					@Override
					public void res(String json, Object sender) {
						// 服务器接收到数据并处理完后反馈应答
						GciDialogManager.getInstance().showTextToast("命令发送成功", GciActivityManager.getInstance().getLastActivity());
						BluetoothMessageManager.getInstance().addMessage("发送命令:" + _lst.get(lstPosition));
					}
				});
			}
		});
	}
}
