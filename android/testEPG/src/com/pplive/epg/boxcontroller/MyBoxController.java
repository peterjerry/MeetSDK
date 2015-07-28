package com.pplive.epg.boxcontroller;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class MyBoxController {
	/**
	 * MSG_REMOTE_TIMEOUT
	 */
	private final static int MSG_REMOTE_TIMEOUT = 3001;
	
	private final static int BYTE_DEFAULT_SIZE = 5;
	private final static int KEYEVENT_BYTE_SIZE = 5;
	private final static int ACTION_KEYEVENT_BYTE_SIZE = 9;
	
	private final static int SOCKET_CREATION_TIMEOUT_MS = 3000;
	
	private Socket mSocket;
	private OutputStream mOs;
	
	private String mIpAddr;
	private int mPort;
	
	private int retry = 0;
	
	public MyBoxController(String ip_addr, int port) {
		mIpAddr = ip_addr;
		mPort = port;
	}
	
	public boolean connect() {
		System.out.println("Java: connect to device " + mIpAddr + ":" + mPort);
		
		try {
			mSocket = new Socket();
			mSocket.setKeepAlive(true);
			mSocket.setTcpNoDelay(true);
			
			InetSocketAddress fullAddr = new InetSocketAddress(mIpAddr, mPort);

	        mSocket.connect(fullAddr, SOCKET_CREATION_TIMEOUT_MS);
			mOs = mSocket.getOutputStream();
			return true;
		}
		catch (SocketTimeoutException e) {
	    	e.printStackTrace();
	    	System.out.println("===SocketTimeoutException====");
	    }
		catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		return false;
	}
	
	public void sendKeyEvent(int key) {
		byte[] mbyte = new byte[BYTE_DEFAULT_SIZE + KEYEVENT_BYTE_SIZE];
		int length = KEYEVENT_BYTE_SIZE;
		mbyte[0] = 'K'; // 以K开头的byte数组存放的是按键事件
		putInt(mbyte, length, 1);
		putInt(mbyte, key, BYTE_DEFAULT_SIZE);
		buildPacket(mbyte, length);
		try
		{
			StringBuffer sb = new StringBuffer();
			for (int i=0;i<mbyte.length;i++)
				sb.append(String.format("0x%02x ", mbyte[i]));
			sb.append(" , content: ");
			sb.append(new String(mbyte, "UTF-8"));
			System.out.println("Java: sendKeyEvent: " + sb.toString());
			
			if (mOs != null && !mSocket.isClosed()) {
				mOs.write(mbyte);
			}
			
			retry = 0;
		}
		catch (IOException e) {
			e.printStackTrace();
			
			if (retry < 3) {
				System.out.println("Java: failed to write to socet, re-connect to device");
				try {
					mSocket.close();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				
				connect();
				sendKeyEvent(key);
			}
			
			retry++;
		}
	}
	
	private void buildPacket(byte[] bb, int len) {
		bb[len + 4] = bb[5];
		for (int i = 1; i < len - 1; i++)
		{
			bb[len + 4] = (byte) (bb[len + 4] ^ bb[5 + i]);
		}
	}
	
	private void putInt(byte[] bb, int x, int index) {
		bb[index + 3] = (byte) (x >> 24);
		bb[index + 2] = (byte) (x >> 16);
		bb[index + 1] = (byte) (x >> 8);
		bb[index + 0] = (byte) (x >> 0);
	}
	
}
