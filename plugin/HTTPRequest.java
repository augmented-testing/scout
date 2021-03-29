// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.io.InputStream;
import java.net.Socket;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Properties;
import java.util.Vector;

public class HTTPRequest
{
	public static final int REQUEST_TYPE_INVALID=-1;
	public static final int REQUEST_TYPE_GET=0;
	public static final int REQUEST_TYPE_POST=1;
	public static final int REQUEST_TYPE_PUT=2;
	public static final int REQUEST_TYPE_PATCH=3;
	public static final int REQUEST_TYPE_DELETE=4;

	private int requestType=REQUEST_TYPE_INVALID;
	private String httpVersion="";
	private String request="";
	private String command="";
	private String commandParamString="";
	private Vector commandParams=new Vector();
	private Properties requestParams=new Properties();
	private String commandAndParams="";
	private byte[] body=null;

	HTTPRequest()
	{
	}

	public String getCommand()
	{
		return command;
	}

	public void setCommand(String command)
	{
		this.command=command;
	}

	public String getHttpVersion()
	{
		return httpVersion;
	}

	public int getRequestType()
	{
		return requestType;
	}

	/**
	 * Creates a HTTP POST request from a request String
	 * of the form command?param1=value1&param2=value2
	 */
	public static String createRequest(String request)
	{
		String command;
		String params;

		int startOfParams=request.indexOf('?');
		if(startOfParams==-1)
		{
			command=request;
			params="";
		}
		else
		{
			command=request.substring(0, startOfParams);
			params=request.substring(startOfParams+1);
		}

		StringBuffer http=new StringBuffer();

		// Create POST request
		http.append("POST /");
		http.append(command);
		http.append(" HTTP/1.1\r\n");
		http.append("Content-Length: ");
		http.append(params.length());
		http.append("\r\n");
		http.append("Content-Type: text/xml\r\n\r\n");
		http.append(params);

		return http.toString();
	}

	/**
	 * Read request command and parameters from a request String
	 * of the form command?param1=value1&param2=value2
	 * @returns true if parsed ok or false if not
	 */
	public boolean readRequest(String request)
	{
		this.request=request;
		Vector commandAndParamsArray=splitString(request, '?');
		command=decode((String)commandAndParamsArray.elementAt(0));
		if(commandAndParamsArray.size()>=2)
		{
			// Command and parameter
			commandParamString=(String)commandAndParamsArray.elementAt(1);
			commandParams=parseParameters(commandParamString);
		}
		return true;
	}

	/**
	 * Read lines until a blank line is found (==end of response)
	 * @returns true if parsed ok or false if not
	 */
	public boolean readRequest(Socket socket)
	{
		try
		{
			InputStream inputStream=socket.getInputStream();

			String header=readLine(inputStream);
			if(header==null)
			{
				// No header to read
				return false;
			}
			
			request=header;

			// GET/command?name=value&name=value HTTP/1.1
			if(header.length()<4)
			{
				// Cannot be valid
				return false;
			}

			// Determine request type
			requestType=getHttpRequestType(header);
			if(requestType==REQUEST_TYPE_INVALID)
			{
				// Neither GET or POST - invalid
				return false;
			}

			httpVersion=getHttpVersion(header);
			if(httpVersion==null)
			{
				return false;
			}

			if(readRequestParameters(inputStream)==false)
			{
				// Failed to read parameters
				return false;
			}
/*
			int startCommandIndex=4;
			if(requestType==REQUEST_TYPE_GET || requestType==REQUEST_TYPE_PUT)
			{
				startCommandIndex=4;
			}
			else if(requestType==REQUEST_TYPE_POST)
			{
				startCommandIndex=5;
			}
			else if(requestType==REQUEST_TYPE_PATCH)
			{
				startCommandIndex=6;
			}
			else if(requestType==REQUEST_TYPE_DELETE)
			{
				startCommandIndex=7;
			}
*/			
			commandAndParams=getHttpCommand(header);
			if(commandAndParams==null)
				return false;

			Vector<String> commandAndParamsArray=splitString(commandAndParams, '?');
			command=decode((String)commandAndParamsArray.elementAt(0));
			if(commandAndParamsArray.size()>=2)
			{
				// Command and parameter
				commandParamString=(String)commandAndParamsArray.elementAt(1);
				commandParams=parseParameters(commandParamString);
			}
			
			if(requestType==REQUEST_TYPE_POST || requestType==REQUEST_TYPE_PUT || requestType==REQUEST_TYPE_PATCH)
			{
				// Read body
				int length=getContentLength();
				body=readBody(inputStream, length);
			}

			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	/**
	 * @return Number of bytes content or 0 if unknown
	 */
	private int getContentLength()
	{
		String propContentLength;

		propContentLength=requestParams.getProperty("Content-Length");
		if(propContentLength!=null)
			return string2Int(propContentLength);

		propContentLength=requestParams.getProperty("Content-length");
		if(propContentLength!=null)
			return string2Int(propContentLength);

		propContentLength=requestParams.getProperty("content-length");
		if(propContentLength!=null)
			return string2Int(propContentLength);

		return 0;
	}

	/**
	 * @return Content type
	 */
	private String getContentType()
	{
		String propContentType;

		propContentType=requestParams.getProperty("Content-Type");
		if(propContentType!=null)
			return propContentType;

		propContentType=requestParams.getProperty("Content-type");
		if(propContentType!=null)
			return propContentType;

		propContentType=requestParams.getProperty("content-type");
		if(propContentType!=null)
			return propContentType;

		return "";
	}

	/**
	 * Reads one line from inStream
	 */
	private String readLine(InputStream inStream)
	{
		ByteBuffer bytes=new ByteBuffer();

		try
		{
			while(true)
			{
				// Stream is ready
				int nextChar=inStream.read();
				if(nextChar==-1)
				{
					// End of stream
					byte[] b=bytes.getByteArray();
					// Create an UTF-8 String
					String utf=new String(b, "UTF-8");
					return utf;
				}
				else if((char)nextChar=='\r')
				{
					// CR - ignore
				}
				else if((char)nextChar=='\n')
				{
					// End of line
					byte[] b=bytes.getByteArray();
					// Create an UTF-8 String
					String utf=new String(b, "UTF-8");
					return utf;
				}
				else
				{
					// Read a byte
					bytes.addByte((byte)nextChar);
				}
			}
		}
		catch(Exception e)
		{
			return "";
		}
	}

	/**
	 * Reads length bytes from inStream
	 * @param inStream
	 * @param length
	 * @return
	 */
	private byte[] readBody(InputStream inStream, int length)
	{
		ByteBuffer bytes=new ByteBuffer();

		try
		{
			for(int i=0; i<length; i++)
			{
				// Stream is ready
				int nextChar=inStream.read();
				bytes.addByte((byte)nextChar);
			}
			byte[] b=bytes.getByteArray();
			return b;
		}
		catch(Exception e)
		{
			return null;
		}
	}

	/**
	 * Retrieve the command embedded in an HTTP get/post request
	 * @param message "GET /command HTTP/1.1"
	 * @return The command or null if not found
	 */
	public static String getHttpCommand(String message)
	{
		int first=message.indexOf("/");
		if(first==-1)
			return "";
		first+=1;
		
		String command="";
		int last=message.lastIndexOf("HTTP/");
		if(last==-1)
			command=message.substring(first);
		else
			command=message.substring(first, last);
		return command.trim();
	}

	/**
	 * @return The HTTP version (HTTP/1.1) or null if not an HTTP message
	 */
	private String getHttpVersion(String message)
	{
		int last=message.indexOf("HTTP/");
		if(last==-1)
			return null;
		return message.substring(last, last+8);
	}

	/**
	 * @return REQUEST_TYPE_POST, REQUEST_TYPE_GET or REQUEST_TYPE_INVALID
	 */
	private int getHttpRequestType(String header)
	{
		if(header.startsWith("GET"))
		{
			// GET request
			return REQUEST_TYPE_GET;
		}
		else if(header.startsWith("POST"))
		{
			// POST request
			return REQUEST_TYPE_POST;
		}
		else if(header.startsWith("PUT"))
		{
			// POST request
			return REQUEST_TYPE_PUT;
		}
		else if(header.startsWith("PATCH"))
		{
			// POST request
			return REQUEST_TYPE_PATCH;
		}
		else if(header.startsWith("DELETE"))
		{
			// POST request
			return REQUEST_TYPE_DELETE;
		}
		else
		{
			// Neither GET or POST - invalid
			return REQUEST_TYPE_INVALID;
		}
	}

	/**
	 * Reads all request parameters
	 */
	private boolean readRequestParameters(InputStream inStream)
	{
		requestParams=new Properties();
		try
		{
			// Read parameters
			String newLine;
			while(true)
			{
				newLine=readLine(inStream);
				if(newLine==null || "".equals(newLine))
				{
					// Empty line or no line - stop reading
					return true;
				}
				else
				{
					// Line read
					int delimiter=newLine.indexOf(':');
					if(delimiter==-1)
					{
						// No delimiter - no parameter
					}
					else
					{
						// Add the new request parameter
						String key=newLine.substring(0, delimiter);
						String value=newLine.substring(delimiter+1);
						value=value.trim();
						requestParams.setProperty(key, value);
					}
				}
			}
		}
		catch(Exception e)
		{
			return false;
		}
	}

	/**
	 * Parses command parameters in the form name=value&name=value
	 */
	public static Vector parseParameters(String param)
	{
		Vector p=new Vector();

		Vector paramArray=splitString(param, '&');
		for(int i=0; i<paramArray.size(); i++)
		{
			Vector propArray=splitString(decode((String)paramArray.elementAt(i)), "=");
			if(propArray.size()==2)
			{
				// Is a key value pair
				String key=(String)propArray.elementAt(0);
				String value=(String)propArray.elementAt(1);
				KeyValue commandParameter=new KeyValue(key, value);
				p.add(commandParameter);
			}
			else if(propArray.size()==1)
			{
				// Is a key without a value
				String key=(String)propArray.elementAt(0);
				String value=null;
				if(key!=null && !"".equals(key))
				{
					// Key must not be null or blank
					KeyValue commandParameter=new KeyValue(key, value);
					p.add(commandParameter);
				}
			}
		}
		return p;
	}

	/**
	 * @return The value of command parameter key or null if not found
	 */
	public String getParameter(String key)
	{
		for(int i=0; i<commandParams.size(); i++)
		{
			KeyValue keyValue=(KeyValue)commandParams.elementAt(i);
			if(key.equalsIgnoreCase(keyValue.getKey()))
				return keyValue.getValue();
		}
		return null;
	}

	/**
	 * @return All parameters in a Properties
	 */
	public Properties getParameters()
	{
		Properties p=new Properties();

		for(int i=0; i<commandParams.size(); i++)
		{
			KeyValue keyValue=(KeyValue)commandParams.elementAt(i);
			if(keyValue.getValue()!=null)
			{
				p.setProperty(keyValue.getKey(), keyValue.getValue());
			}
		}
		return p;
	}

	/**
	 * Replaces old key if exists add the key otherwise
	 */
	public void setParameter(String key, String value)
	{
		for(int i=0; i<commandParams.size(); i++)
		{
			KeyValue keyValue=(KeyValue)commandParams.elementAt(i);
			if(key.equals(keyValue.getKey()))
			{
				// Replace value
				keyValue=new KeyValue(key, value);
				commandParams.setElementAt(keyValue, i);
				return;
			}
		}
		addParameter(key, value);
	}

	/**
	 * Add a command parameter
	 */
	public boolean addParameter(String key, String value)
	{
		try
		{
			KeyValue keyValue=new KeyValue(key, value);
			commandParams.add(keyValue);
		}
		catch(Throwable e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Returns the URL encoded HTTP command to send to the server
	 * @return A command String of the form command?param1=value1&param2=value2 (URL encoded after ?)
	 */
	public String getCommandString()
	{
		StringBuffer buf=new StringBuffer();

		buf.append(encode(command));
		buf.append("?");
		buf.append(getCommandParamString());
		return decode(buf.toString());
	}

	/**
	 * Returns the command String (URL encoded)
	 * Does not add an equal sign for parameters with null values
	 * @return A command parameter String of the form param1=value1&param2=value2
	 */
	public String getCommandParamString()
	{
		return decode(commandParamString);
	}

	/**
	 * @return A vector of stings splitted with a delimiter
	 */
	public static Vector<String> splitString(String s, char delimiter)
	{
		Vector<String> v=new Vector<String>();
		StringBuffer b=new StringBuffer();
		for(int i=0; i<s.length(); i++)
		{
			char nextChar=s.charAt(i);
			if(nextChar==delimiter)
			{
				// Add this to the vector
				v.add(b.toString());
				b=new StringBuffer();
			}
			else
			{
				b.append(nextChar);
			}
		}
		// Add the last piece to the vector
		v.add(b.toString());
		return v;
	}

	public static int string2Int(String s)
	{
		return string2Int(s, 0);
	}

	public static int string2Int(String s, int otherwise)
	{
		try
		{
			return Integer.parseInt(s);
		}
		catch(Exception e)
		{
			// Couldn't convert to int
			return otherwise;
		}
	}

	/**
	 * Split the String s in two pieces with delimiter
	 * Will always return a Vector of two Strings if delimiter was found
	 * one item if delimiter not found
	 * @param s
	 * @param delimiter
	 * @return
	 */
	public static Vector splitString(String s, String delimiter)
	{
		Vector v=new Vector();
		for(int i=0; i<s.length(); i++)
		{
			int pos=s.indexOf(delimiter);
			if(pos>-1)
			{
				// Delimiter found - split
				v.add(s.substring(0, pos));
				if(pos+delimiter.length()>=s.length())
					v.add("");
				else
					v.add(s.substring(pos+delimiter.length()));
				return v;
			}
		}
		v.add(s);
		return v;
	}

	/**
	 * Converts from an url encoded String to a Java String
	 */
	public static String decode(String s)
	{
		try
		{
			return URLDecoder.decode(s, "UTF-8");
		}
		catch(Exception e)
		{
			return "";
		}
	}

	public static String encode(String s)
	{
		try
		{
			return URLEncoder.encode(s, "UTF-8");
		}
		catch(Exception e)
		{
			return "";
		}
	}

	public byte[] getBody()
	{
		return body;
	}

	public void setBody(byte[] body)
	{
		this.body = body;
	}

	public Properties getRequestParams()
	{
		return requestParams;
	}

	/**
	 * @return The entire request string, for example: "GET /command HTTP/1.1"
	 */
	public String getRequest()
	{
		return request;
	}

	/**
	 * @return Commands and parameters in the request
	 */
	public String getCommandAndParams()
	{
		return commandAndParams;
	}

	public String getRequestComparator()
	{
		StringBuffer buf=new StringBuffer();
		buf.append(command);
		buf.append("?");
		for(int i=0; i<commandParams.size(); i++)
		{
			if(i>0)
			{
				buf.append("&");
			}
			KeyValue keyValue=(KeyValue)commandParams.elementAt(i);
			buf.append(keyValue.getKey()+"={param"+(i+1)+"}");
		}
		return buf.toString();
	}
}
