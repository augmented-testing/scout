package plugin;

public class KeyValue
{
	private String key;
	private String value;

	KeyValue(String key, String value)
	{
		this.key=key;
		this.value=value;
	}

	public String getKey()
	{
		return key;
	}

	public String getValue()
	{
		return value;
	}
}
