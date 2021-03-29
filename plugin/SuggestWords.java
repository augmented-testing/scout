// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import scout.AppState;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetSubtype;
import scout.Widget.WidgetType;
import scout.Widget.WidgetVisibility;

public class SuggestWords
{
	public void updateSuggestions()
	{
		AppState currentState=StateController.getCurrentState();
		List<Widget> suggestions=new ArrayList<Widget>();
		List<String> suggestedValues=new ArrayList<String>();
		List<Widget> widgets=currentState.getVisibleWidgets();
		for(Widget widget:widgets)
		{
			if(widget.getWidgetType()==WidgetType.ACTION && widget.getWidgetSubtype()==WidgetSubtype.TYPE_ACTION && widget.getWidgetVisibility()==WidgetVisibility.VISIBLE)
			{
				// Get mutations of widget
		  	List<Widget> clonesAndMutations=StateController.getClonesAndMutations(widget);

				// Get values from all clones and mutations
				List<String> allValues=new ArrayList<String>();
				for(Widget widgetClone:clonesAndMutations)
				{
					if(widgetClone.getText()!=null)
					{
						allValues.add(removeTags(widgetClone.getText()));
					}
				}
				
				if(allValues.size()>0)
				{
					// We have previous values
					if(isWordList(allValues))
					{
						// Single words
						List<List<Category>> categorysList=new ArrayList<List<Category>>();
						for(Widget clone:clonesAndMutations)
						{
							@SuppressWarnings("unchecked")
							List<Category> categories=(List<Category>)widget.getMetadata("categories");
							if(categories!=null)
							{
								// Has categories already
								categorysList.add(categories);
							}
							else
							{
								// Get categories for widget
								categories=getCategories(removeTags(clone.getText()));
								if(categories!=null && categories.size()>0)
								{
									widget.putMetadata("categories", categories);
									categorysList.add(categories);
								}
							}
						}
						
						Category mostCommonCategory=mostCommonCategory(categorysList);
						if(mostCommonCategory!=null)
						{
							for(String wordSuggestion:mostCommonCategory.getWords())
							{
								if(!containsValue(allValues, wordSuggestion) && !containsValue(suggestedValues, wordSuggestion))
								{
									Widget suggestion=new Widget(widget);
									suggestion.setWidgetVisibility(WidgetVisibility.SUGGESTION);
									suggestion.setText(wordSuggestion);
									suggestion.setComment(StateController.translate("SinceSimilarValue"));
									suggestion.setPriority(0);
									suggestions.add(suggestion);
									suggestedValues.add(wordSuggestion);
								}
							}
						}
					}
				}
			}
		}
		if(suggestions.size()>0)
		{
			currentState.replaceSuggestedWidgets(suggestions, "SuggestWords");
		}
	}

	public List<String> getSuggestedWords(List<String> examples)
	{
		String exampleString="";
		for(int i=0; i<examples.size() && i<1; i++)
		{
			if(i>0)
			{
				exampleString+=" ";
			}
			String example=examples.get(i).trim();
			exampleString+=example;
		}

		List<String> suggestions=new ArrayList<String>();
		HttpServiceCaller service=new HttpServiceCaller();
		String request="http://api.conceptnet.io/c/en/"+exampleString;
		String response=service.executeGetRequest(request);
		if(response!=null)
		{
			int pos=0;
			for(int i=0; i<3; i++)
			{
				int indexIs=response.indexOf("/IsA/", pos);
				if(indexIs>=0)
				{
					int index=response.indexOf("term", indexIs+5);
					if(index>=0)
					{
						int endIndex=response.indexOf("\"", index+74);
						if(endIndex>=0)
						{
							String url=response.substring(index+74, endIndex);
							List<String> words=getWords(url);
							for(String word:words)
							{
								suggestions.add(word);
							}
							pos=endIndex;
						}
					}
				}
			}
		}
		
		return suggestions;
	}

	public List<Category> getCategories(String exampleWord)
	{
		List<Category> categories=new ArrayList<Category>();
		HttpServiceCaller service=new HttpServiceCaller();
		String request="http://api.conceptnet.io/c/en/"+exampleWord;
		String response=service.executeGetRequest(request);
		if(response!=null)
		{
			int pos=0;
			for(int i=0; i<3; i++)
			{
				int indexIs=response.indexOf("/IsA/", pos);
				if(indexIs>=0)
				{
					int index=response.indexOf("term", indexIs+5);
					if(index>=0)
					{
						int endIndex=response.indexOf("\"", index+74);
						if(endIndex>=0)
						{
							String url=response.substring(index+74, endIndex);
							if(url.startsWith("/c/en/"))
							{
								// English category
								Category category=new Category(url);
								List<String> words=getWords(url);
								for(String word:words)
								{
									category.addWord(word);
								}
								categories.add(category);
								pos=endIndex;
							}
						}
					}
				}
			}
		}
		return categories;
	}

	private List<String> getWords(String url)
	{
		List<String> words=new ArrayList<String>();
		HttpServiceCaller service=new HttpServiceCaller();
		String request="http://api.conceptnet.io"+url;
		String response=service.executeGetRequest(request);
		if(response!=null)
		{
			int pos=0;
			for(int i=0; i<3; i++)
			{
				int indexStart=response.indexOf("start", pos);
				if(indexStart>=0)
				{
					int index=response.indexOf("label", indexStart+5);
					if(index>=0)
					{
						int endIndex=response.indexOf("&quot;", index+66);
						if(endIndex>=0)
						{
							String word=response.substring(index+66, endIndex);
							words.add(word);
							pos=endIndex;
						}
					}
				}
			}
		}
		return words;
	}

	private boolean isWordList(List<String> list)
	{
		for(String item:list)
		{
			if(!isWord(item))
			{
				return false;
			}
		}
		return true;
	}

	private boolean isWord(String text)
	{
		for(int i=0; i<text.length(); i++)
		{
			char c=text.charAt(i);
			if(!Character.isAlphabetic(c))
			{
				return false;
			}
		}
		return true;
	}

	private boolean containsValue(List<String> list, String value)
	{
		for(String item:list)
		{
			if(item.trim().equalsIgnoreCase(value.trim()))
			{
				return true;
			}
		}
		return false;
	}

	public List<Category> commonCategories(List<List<Category>> categoriesList)
	{
		List<Category> commonCategories=new ArrayList<Category>();
		for(List<Category> categories:categoriesList)
		{
			if(commonCategories.size()==0)
			{
				if(categories!=null)
				{
					commonCategories.addAll(categories);
				}
			}
			else
			{
				// We have common categories
				if(categories!=null)
				{
					List<Category> stillCommonCategories=new ArrayList<Category>();
					for(Category category:categories)
					{
						if(commonCategories.contains(category))
						{
							// Still a common category
							stillCommonCategories.add(category);
						}
					}
					if(stillCommonCategories.size()==0)
					{
						// No common categories
						return stillCommonCategories;
					}
					commonCategories=stillCommonCategories;
				}
			}
		}
		return commonCategories;
	}

	public Category mostCommonCategory(List<List<Category>> categoriesList)
	{
		Hashtable<Category, Integer> categoryCount=new Hashtable<Category, Integer>();
		for(List<Category> categories:categoriesList)
		{
			for(Category category:categories)
			{
				Integer count=categoryCount.get(category);
				if(count==null)
				{
					categoryCount.put(category, 1);
				}
				else
				{
					categoryCount.put(category, count+1);
				}
			}
		}

		Category commonCategory=null;
		int commonCount=0;
		Set<Category> keys=categoryCount.keySet();
		for(Category key:keys)
		{
			if(commonCategory==null)
			{
				commonCategory=key;
				commonCount=categoryCount.get(key);
			}
			else
			{
				int count=categoryCount.get(key);
				if(count>commonCount)
				{
					commonCategory=key;
					commonCount=count;
				}
			}
		}

		return commonCategory;
	}

	private String removeTags(String text)
	{
		text=text.replace("[ENTER]", "");
		text=text.replace("[TAB]", "");
		return text;
	}
}
