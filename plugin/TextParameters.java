package plugin;

import java.util.List;

public class TextParameters
{
	private String text=null;
	private List<KeyValue> params=null;

	public TextParameters(String text, List<KeyValue> params)
	{
		this.text = text;
		this.params = params;
	}

	public String getText()
	{
		return text;
	}

	public void setText(String text)
	{
		this.text = text;
	}

	public List<KeyValue> getParams()
	{
		return params;
	}

	public void setParams(List<KeyValue> params)
	{
		this.params = params;
	}
}
