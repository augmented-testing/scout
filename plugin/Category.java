package plugin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Category implements Serializable
{
	private static final long serialVersionUID = -3838693555613292426L;
	private String name=null;
	private List<String> words=new ArrayList<String>();

	public Category(String name)
	{
		this.name = name;
	}

	public void addWord(String word)
	{
		words.add(word);
	}
	
	public List<String> getWords()
	{
		return words;
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (getClass() != o.getClass())
			return false;
		Category category=(Category)o;
		return name.equals(category.getName());
	}
}
