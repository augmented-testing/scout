// SPDX-FileCopyrightText: 2021 Michel Nass
//
// SPDX-License-Identifier: MIT

package plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import scout.AppState;
import scout.StateController;

public class SerializeState
{
	public static final String DATA_FILEPATH = "data";
	public static final String BRAIN_FILENAME = "brain";
	public static final String PRODUCT_PROPERTIES_FILE = "product.properties";

	public SerializeState()
	{
		StateController.setProducts(getFolders());
	}
	
	/**
	 * Save the state tree for the current product
	 * @return true if done
	 */
	public Boolean saveState()
	{
		AppState stateTree=StateController.getStateTree();
		String product=StateController.getProduct();

		String filePath;
		if(product.length()>0)
		{
			filePath=DATA_FILEPATH+"/"+product;
		}
		else
		{
			filePath=DATA_FILEPATH;
		}
		
		// Make sure that folders exist
		File file=new File(filePath);
		file.mkdirs();

		// Save product properties in the same folder
		saveProductProperties(product, filePath);
		
		// Add filename
		filePath+="/"+BRAIN_FILENAME;

		// Save state tree
		if(!saveObject(filePath, stateTree))
		{
			return false;
		}
		
		// Get weekday number
		Calendar c = Calendar.getInstance();
		c.setTime(new Date());
		int dayOfWeek = c.get(Calendar.DAY_OF_WEEK);
		
		// Save a backup
		filePath+=dayOfWeek;
		if(!saveObject(filePath, stateTree))
		{
			return false;
		}

		// Update products
		StateController.setProducts(getFolders());
		
		return true;
	}

	/**
	 * Load state tree for for the current product or create a new home state if not found
	 * @return A state tree
	 */
	public AppState loadState()
	{
		String product=StateController.getProduct();
		String filePath;
		if(product.length()>0)
		{
			filePath=DATA_FILEPATH+"/"+product;
		}
		else
		{
			filePath=DATA_FILEPATH;
		}

		// Load product properties from the same folder
		loadProductProperties(product, filePath);

		// Add filename
		filePath+="/"+BRAIN_FILENAME;

		// Load the model
		AppState stateTree=null;
		Object object=loadObject(filePath);
		if(object!=null)
		{
			stateTree=(AppState)object;
		}
		else
		{
			// Create a new state
			stateTree=new AppState("0", "Home");
		}
		return stateTree;
	}

	private Object loadObject(String filepath)
	{
		try
		{
			FileInputStream fileIn = new FileInputStream(filepath);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Object object = in.readObject();
			in.close();
			fileIn.close();
			return object;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private boolean saveObject(String filepath, Object object)
	{
		try
		{
			FileOutputStream fileOut = new FileOutputStream(filepath);
			ObjectOutputStream out = new ObjectOutputStream(fileOut);
			out.writeObject(object);
			out.flush();
			out.close();
			fileOut.close();
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	private boolean loadProductProperties(String product, String projectRootPath)
	{
		Properties productProperties = new Properties();
		try
		{
			String filepath = projectRootPath + "/" + PRODUCT_PROPERTIES_FILE;
			FileInputStream in = new FileInputStream(filepath);
			productProperties.load(in);
			in.close();
			StateController.setProductProperties(productProperties);
			return true;
		}
		catch (Exception e)
		{
			StateController.setProductProperties(productProperties);
			return false;
		}
	}

	private boolean saveProductProperties(String product, String projectRootPath)
	{
		try
		{
			String filepath = projectRootPath + "/" + PRODUCT_PROPERTIES_FILE;
			FileOutputStream out = new FileOutputStream(filepath);
			if (out != null)
			{
				StateController.getProductProperties().store(out, null);
				out.close();
			}

			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * @return All folders in the data folder
	 */
	private List<String> getFolders()
	{
		File scriptsFile=new File(DATA_FILEPATH);
		List<String> filenames=getFolders(scriptsFile);
		return filenames;
	}
	
	private List<String> getFolders(File folder)
	{
		List<String> filenames=new ArrayList<String>();
		try
		{
			File[] listOfFiles = folder.listFiles();
			for (int i = 0; i < listOfFiles.length; i++)
			{
				if (listOfFiles[i].isDirectory())
				{
					String filename = listOfFiles[i].getName();
					filenames.add(filename);
				}
			}
			return filenames;
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
