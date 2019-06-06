package ipPhone_util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/*
 * 这个是一个公共类
 * 用于将可序列化的对象转换成字节数组
 **/
public class DataTransfer {
	
	public static byte[] serializableObjectToByteArray(Object o) {
		byte[] serializedMessage = null;
		try {
			ByteArrayOutputStream bStream = new ByteArrayOutputStream();
			ObjectOutput oo = new ObjectOutputStream(bStream);
			oo.writeObject(o);
			oo.close();
			serializedMessage = bStream.toByteArray();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return serializedMessage;
	}
	
	public static Object byteArrayToSerializableObject(byte[] b) {
		if( b == null)
			return null;
		if(b.length == 0)
			return null;
		try {
			return new ObjectInputStream(new ByteArrayInputStream(b)).readObject();
		} catch (IOException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

}
