package com.hcm.camera.net.model;

public class TransDataBase {

	public byte[] sendBytes;

	public byte type;

	public int length;

	public transient boolean isNeedReSend = false;

	public transient long sendtime = 0;

	public transient String fun = "";

	public transient String messageno = "";

	public transient int tag = 0;

	public transient int re_send_count = 0;

	public transient int online_re_send_count = 0;
}
