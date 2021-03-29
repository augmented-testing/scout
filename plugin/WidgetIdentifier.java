// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.util.ArrayList;
import java.util.List;

import scout.StateController;
import scout.Widget;

/**
 * This class can be customized to define what elements to extract and how to classify different types of widgets.
 */
public class WidgetIdentifier
{
	public static final String ELEMENTS_TO_EXTRACT="input textarea button select a h1 h2 h3 h4 h5 li span div p th tr td frame iframe label svg";
	public static final String CONTAINER_TAGS="div";
	public static final String FRAME_TAGS="frame iframe";
	public static final String CLICK_TAGS="button a svg";
	public static final String CLICK_INPUT_TYPES="submit reset radio checkbox";
	public static final String MOVE_TAGS="a";
	public static final String SELECT_TAGS="select";
	public static final String CHECK_TAGS="h1 h2 h3 h4 h5 li span div p th td label textarea";
	public static final String CHECK_INPUT_TYPES="text email month number range search tel time url week password";
	public static final String TYPE_TAGS="textarea";
	public static final String TYPE_INPUT_TYPES="text color date datetime-local email month number range search tel time url week password";

	private static String elementsToExtract=null;
	private static List<String> containerTagArray=null;
	private static List<String> frameTagArray=null;
	private static List<String> clickTagArray=null;
	private static List<String> clickInputTypeArray=null;
	private static List<String> clickClassesArray=null;
	private static List<String> moveTagArray=null;
	private static List<String> moveClassesArray=null;
	private static List<String> selectTagArray=null;
	private static List<String> selectClassesArray=null;
	private static List<String> checkTagArray=null;
	private static List<String> checkInputTypeArray=null;
	private static List<String> checkClassesArray=null;
	private static List<String> typeTagArray=null;
	private static List<String> typeInputTypeArray=null;
	private static List<String> typeClassesArray=null;

	public static void initIdentifiers()
	{
		String elements=StateController.getProductProperty("elements_to_extract", ELEMENTS_TO_EXTRACT);
		elementsToExtract=spaceToComma(elements);

		containerTagArray=splitSpace(StateController.getProductProperty("container_tags", CONTAINER_TAGS));

		frameTagArray=splitSpace(StateController.getProductProperty("frame_tags", FRAME_TAGS));

		clickTagArray=splitSpace(StateController.getProductProperty("click_tags", CLICK_TAGS));
		clickInputTypeArray=splitSpace(StateController.getProductProperty("click_input_types", CLICK_INPUT_TYPES));
		clickClassesArray=splitSpace(StateController.getProductProperty("click_classes", null));

		moveTagArray=splitSpace(StateController.getProductProperty("move_tags", MOVE_TAGS));
		moveClassesArray=splitSpace(StateController.getProductProperty("move_classes", null));

		selectTagArray=splitSpace(StateController.getProductProperty("select_tags", SELECT_TAGS));
		selectClassesArray=splitSpace(StateController.getProductProperty("select_classes", null));

		checkTagArray=splitSpace(StateController.getProductProperty("check_tags", CHECK_TAGS));
		checkInputTypeArray=splitSpace(StateController.getProductProperty("check_input_types", CHECK_INPUT_TYPES));
		checkClassesArray=splitSpace(StateController.getProductProperty("check_classes", null));

		typeTagArray=splitSpace(StateController.getProductProperty("type_tags", TYPE_TAGS));
		typeInputTypeArray=splitSpace(StateController.getProductProperty("type_input_types", TYPE_INPUT_TYPES));
		typeClassesArray=splitSpace(StateController.getProductProperty("type_classes", null));
	}

	public void startTool()
	{
		WidgetIdentifierDialog dialog = new WidgetIdentifierDialog(StateController.getParentFrame());
		dialog.showDialog();
	}

	public static String getElementsToExtract()
	{
		return elementsToExtract;
	}

	public static boolean isContainerWidget(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && containerTagArray!=null)
		{
			for(String s:containerTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isFrame(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && frameTagArray!=null)
		{
			for(String s:frameTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		return false;
	}

	public static boolean isClickWidget(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && clickTagArray!=null)
		{
			for(String s:clickTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String type=(String)widget.getMetadata("type");
		if(tag!=null && type!=null && tag.equalsIgnoreCase("input"))
		{
			for(String s:clickInputTypeArray)
			{
				if(type.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String className=(String)widget.getMetadata("class");
		if(className!=null && clickClassesArray!=null && clickClassesArray.size()>0)
		{
			List<String> classNames=splitSpace(className);
			for(String name:classNames)
			{
				for(String s:clickClassesArray)
				{
					if(name.equalsIgnoreCase(s))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean isMoveWidget(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && moveTagArray!=null)
		{
			for(String s:moveTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String className=(String)widget.getMetadata("class");
		if(className!=null && moveClassesArray!=null && moveClassesArray.size()>0)
		{
			List<String> classNames=splitSpace(className);
			for(String name:classNames)
			{
				for(String s:moveClassesArray)
				{
					if(name.equalsIgnoreCase(s))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean isSelectWidget(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && selectTagArray!=null)
		{
			for(String s:selectTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String className=(String)widget.getMetadata("class");
		if(className!=null && selectClassesArray!=null && selectClassesArray.size()>0)
		{
			List<String> classNames=splitSpace(className);
			for(String name:classNames)
			{
				for(String s:selectClassesArray)
				{
					if(name.equalsIgnoreCase(s))
					{
						return true;
					}
				}
			}
		}

		return false;
	}

	public static boolean isCheckWidget(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && checkTagArray!=null)
		{
			for(String s:checkTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String type=(String)widget.getMetadata("type");
		if(tag!=null && type!=null && tag.equalsIgnoreCase("input"))
		{
			for(String s:checkInputTypeArray)
			{
				if(type.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String className=(String)widget.getMetadata("class");
		if(className!=null && checkClassesArray!=null && checkClassesArray.size()>0)
		{
			List<String> classNames=splitSpace(className);
			for(String name:classNames)
			{
				for(String s:checkClassesArray)
				{
					if(name.equalsIgnoreCase(s))
					{
						return true;
					}
				}
			}
		}

		return false;
	}
	
	public static boolean isTypeWidget(Widget widget)
	{
		String tag=(String)widget.getMetadata("tag");

		if(tag!=null && typeTagArray!=null)
		{
			for(String s:typeTagArray)
			{
				if(tag.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String type=(String)widget.getMetadata("type");
		if(tag!=null && type!=null && tag.equalsIgnoreCase("input"))
		{
			for(String s:typeInputTypeArray)
			{
				if(type.equalsIgnoreCase(s))
				{
					return true;
				}
			}
		}

		String className=(String)widget.getMetadata("class");
		if(className!=null && typeClassesArray!=null && typeClassesArray.size()>0)
		{
			List<String> classNames=splitSpace(className);
			for(String name:classNames)
			{
				for(String s:typeClassesArray)
				{
					if(name.equalsIgnoreCase(s))
					{
						return true;
					}
				}
			}
		}

		return false;
	}
	
	private static String spaceToComma(String spaceSeparated)
	{
		StringBuffer buf=new StringBuffer(); 
		String[] array=spaceSeparated.trim().split("\\s+");
		for(int i=0; i<array.length; i++)
		{
			String s=array[i].trim();
			if(s.length()>0)
			{
				if(buf.length()>0)
				{
					buf.append(',');
				}
				buf.append(s);
			}
		}
		return buf.toString();
	}

	private static List<String> splitSpace(String spaceSeparated)
	{
		List<String> list=new ArrayList<String>();
		if(spaceSeparated!=null)
		{
			String[] array=spaceSeparated.trim().split("\\s+");
			for(int i=0; i<array.length; i++)
			{
				String s=array[i].trim();
				if(s.length()>0)
				{
					list.add(s);
				}
			}
		}
		return list;
	}

	public static void setElementsToExtract(String text)
	{
		StateController.setProductProperty("elements_to_extract", text);
		elementsToExtract=spaceToComma(text);
	}

	public static void setContainerTags(String text)
	{
		StateController.setProductProperty("container_tags", text);
		containerTagArray = splitSpace(text);
	}

	public static void setFrameTags(String text)
	{
		StateController.setProductProperty("frame_tags", text);
		frameTagArray = splitSpace(text);
	}

	public static void setClickTags(String text)
	{
		StateController.setProductProperty("click_tags", text);
		clickTagArray = splitSpace(text);
	}

	public static void setClickInputTypes(String text)
	{
		StateController.setProductProperty("click_input_types", text);
		clickInputTypeArray = splitSpace(text);
	}

	public static void setClickClasses(String text)
	{
		StateController.setProductProperty("click_classes", text);
		clickClassesArray = splitSpace(text);
	}

	public static void setCheckTags(String text)
	{
		StateController.setProductProperty("check_tags", text);
		checkTagArray = splitSpace(text);
	}

	public static void setCheckInputTypes(String text)
	{
		StateController.setProductProperty("check_input_types", text);
		checkInputTypeArray = splitSpace(text);
	}

	public static void setCheckClasses(String text)
	{
		StateController.setProductProperty("check_classes", text);
		checkClassesArray = splitSpace(text);
	}

	public static void setTypeTags(String text)
	{
		StateController.setProductProperty("type_tags", text);
		typeTagArray = splitSpace(text);
	}

	public static void setTypeInputTypes(String text)
	{
		StateController.setProductProperty("type_input_types", text);
		typeInputTypeArray = splitSpace(text);
	}

	public static void setTypeClasses(String text)
	{
		StateController.setProductProperty("type_classes", text);
		typeClassesArray = splitSpace(text);
	}

	public static void setMoveTags(String text)
	{
		StateController.setProductProperty("move_tags", text);
		moveTagArray = splitSpace(text);
	}

	public static void setMoveClasses(String text)
	{
		StateController.setProductProperty("move_classes", text);
		moveClassesArray = splitSpace(text);
	}

	public static void setSelectTags(String text)
	{
		StateController.setProductProperty("select_tags", text);
		selectTagArray = splitSpace(text);
	}

	public static void setSelectClasses(String text)
	{
		StateController.setProductProperty("select_classes", text);
		selectClassesArray = splitSpace(text);
	}
}
