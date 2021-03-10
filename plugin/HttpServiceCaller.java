package plugin;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;

public class HttpServiceCaller
{
	private String responseMessage = "";
	private byte[] mediaBuffer = new byte[10000];

	/**
	 * Executes a HTTP GET request
	 * @param url
	 * @return Text returned from the request or error message
	 */
	public String executeGetRequest(String url)
	{
		return executeGetRequest(url, null);
	}
	
	public String executeGetRequest(String url, Proxy proxy)
	{
		HttpURLConnection conn = null;
		try
		{
			URL u = new URL(url);
			if(proxy==null)
			{
				conn = (HttpURLConnection) u.openConnection();
			}
			else
			{
				conn = (HttpURLConnection) u.openConnection(proxy);
			}
			conn.setRequestMethod("GET");

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line;
			StringBuffer result = new StringBuffer();
			for (int i = 0; (line = rd.readLine()) != null; i++)
			{
				if (i > 0)
				{
					result.append("\r\n");
				}
				result.append(line);
			}
			rd.close();

			return result.toString();
		}
		catch (Exception e)
		{
			responseMessage = "Failed: " + e.toString();
			return null;
		}
	}

	/**
	 * Executes a HTTP GET request
	 * @param url
	 * @return Bytes returned from the request or null
	 */
	public byte[] executeGetMediaRequest(String url)
	{
		HttpURLConnection conn = null;
		try
		{
			URL u = new URL(url);
			conn = (HttpURLConnection) u.openConnection();
			conn.setRequestMethod("GET");

			// Get the response
			DataInputStream dataInputStream=new DataInputStream(conn.getInputStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			int k;
			while((k=dataInputStream.read(mediaBuffer)) != -1) 
			{
				out.write(mediaBuffer, 0, k);
			}

			dataInputStream.close();

			byte[] result = out.toByteArray();
			out.close();
			return result;
		}
		catch (Exception e)
		{
			responseMessage = "Failed: " + e.toString();
			return null;
		}
	}
	
	/**
	 * Executes a HTTP POST request
	 * @param url
	 * @param body The POST body
	 * @return Text returned from the request or error message
	 */
	public String executePostRequest(String url, byte[] body)
	{
		HttpURLConnection conn = null;
		try
		{
			URL u = new URL(url);
			conn = (HttpURLConnection) u.openConnection();
			conn.setRequestMethod("POST");

         conn.setDoOutput(true);
         DataOutputStream dataOutputStream=new DataOutputStream(conn.getOutputStream());
         dataOutputStream.write(body);
         dataOutputStream.flush();

			// Get the response
			BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
			String line;
			StringBuffer result = new StringBuffer();
			for (int i = 0; (line = rd.readLine()) != null; i++)
			{
				if (i > 0)
				{
					result.append("\r\n");
				}
				result.append(line);
			}

			dataOutputStream.close();
			rd.close();

			return result.toString();
		}
		catch (Exception e)
		{
			responseMessage = "Failed: " + e.toString();
			return null;
		}
	}

	/**
	 * @return The response returned from the request
	 */
	public String getResponse()
	{
		return responseMessage;
	}
}
