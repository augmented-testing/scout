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
import java.util.List;
import java.util.Properties;

import javax.swing.JFileChooser;

import scout.AppState;
import scout.Path;
import scout.PathStep;
import scout.StateController;
import scout.Widget;
import scout.Widget.WidgetType;

public class Hive
{
	public static final String MODEL_FILENAME = "brain";
	public static final String PRODUCT_PROPERTIES_FILE = "product.properties";

	private static String hiveFolder=null;
	private static String loadedModelFilename=null;
	private static List<File> modelFiles=null;
	
	public Hive()
	{
		hiveFolder=StateController.getSystemProperty("hive_folder", "hive");
		StateController.setProducts(getFolders());
	}

	public void enablePlugin()
	{
		JFileChooser chooser = new JFileChooser();
    chooser.setCurrentDirectory(new java.io.File("."));
    chooser.setDialogTitle("Select hive folder");
    chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    chooser.setAcceptAllFileFilterUsed(false);

    if(chooser.showOpenDialog(null)==JFileChooser.APPROVE_OPTION)
    {
      File currentDirectory=chooser.getSelectedFile();
      hiveFolder=currentDirectory.getAbsolutePath();
      StateController.setSystemProperty("hive_folder", hiveFolder);
    }
	}

	/**
	 * Save the state tree for the current product
	 * @return true if done
	 */
	public Boolean saveState()
	{
		AppState stateTree=StateController.getStateTree();
		String product=StateController.getProduct();
		
		Path path=StateController.getCurrentPath();
		if(path!=null && path.getWidgets().size()==0)
		{
			// No actions performed - no need to save
			return true;
		}

		String filePath;
		if(product!=null && product.length()>0)
		{
			filePath=hiveFolder+"/"+product;
		}
		else
		{
			filePath=hiveFolder;
		}
		
		// Make sure that folders exist
		File file=new File(filePath);
		file.mkdirs();

		// Save product properties in the same folder
		saveProductProperties(product, filePath);

		// Update products
		StateController.setProducts(getFolders());
		
		if(modelFiles!=null)
		{
			// Merge any recently saved models to avoid lost information
			mergeModels(modelFiles, filePath, stateTree);
		}

		// Save model with a new filename
		String saveModelFilename=filePath+"/"+MODEL_FILENAME+"_"+System.currentTimeMillis();
		saveObject(saveModelFilename, stateTree);

		return true;
	}

	/**
	 * Merge models that have been saved since the model was loaded
	 * @param modelFiles
	 * @param filePath
	 * @param stateTree
	 */
	private void mergeModels(List<File> modelFiles, String filePath, AppState stateTree)
	{
		List<File> currentModelFiles=getFolderFiles(filePath);
		for(File file:currentModelFiles)
		{
			if(!containsFile(file, modelFiles))
			{
				// File saved since model was loaded - merge models
				try
				{
					String hiveModelFilename=filePath+"/"+file.getName();
					Object hiveModelObject=loadObject(hiveModelFilename);
					if(hiveModelObject!=null)
					{
						Object previousObject=loadObject(loadedModelFilename);
						if(previousObject!=null)
						{
							AppState previousStateTree=(AppState)previousObject;
							AppState hiveStateTree=(AppState)hiveModelObject;
							mergeStates(previousStateTree, hiveStateTree, stateTree);
						}
					}
				}
				catch(Exception e)
				{
				}
			}
		}
	}
	
	private boolean containsFile(File file, List<File> files)
	{
		String name=file.getName();
		for(File f:files)
		{
			if(name.equals(f.getName()))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Load model for the current product or create a new home state if not found
	 * @return A state tree
	 */
	public AppState loadState()
	{
		String product=StateController.getProduct();
		String filePath;
		if(product.length()>0)
		{
			filePath=hiveFolder+"/"+product;
		}
		else
		{
			filePath=hiveFolder;
		}

		// Make sure that folders exist
		File file=new File(filePath);
		file.mkdirs();

		// Load product properties from the same folder
		loadProductProperties(product, filePath);

		AppState stateTree=null;

		// Load the model
		String hiveModelFilename=getHiveModelFilename(filePath);
		if(hiveModelFilename!=null)
		{
			// Load model from central storage
			loadedModelFilename=hiveModelFilename;
			Object object=loadObject(hiveModelFilename);
			if(object!=null)
			{
				// Cast to an application state
				stateTree=(AppState)object;
			}
		}
		
		if(stateTree==null)
		{
			// No model loaded - create a new state
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
	 * @return All folders in the hive folder
	 */
	private List<String> getFolders()
	{
		File folder=new File(hiveFolder);
		List<String> filenames=getFolders(folder);
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

	/**
	 * Get the latest model from filePath
	 * @param filePath
	 * @return The latest model (with highest number)
	 */
	private String getHiveModelFilename(String filePath)
	{
		modelFiles=getFolderFiles(filePath);
    String brainFilename=getLastModelFilename(modelFiles);
    if(brainFilename==null)
    {
    	return null;
    }
    return filePath+"/"+brainFilename;
	}

	/**
	 * Get the model file with the highest name
	 * @param files
	 * @return A filename or null
	 */
	private String getLastModelFilename(List<File> files)
	{
		String greatestFilename=null;
		long greatestModelNo=-1;
    for(File file:files)
    {
    	String filename=file.getName();
    	if(filename.startsWith(MODEL_FILENAME+"_"))
    	{
    		int index=filename.lastIndexOf('_');
    		String last=filename.substring(index+1);
    		long modelNo=Long.parseLong(last);
    		if(modelNo>greatestModelNo)
    		{
    			// No filename yet or greater than before
    			greatestFilename=filename;
    			greatestModelNo=modelNo;
    		}
    	}
    }
    return greatestFilename;
	}
	
	private List<File> getFolderFiles(String folderName)
	{
		List<File> files=new ArrayList<File>();
		File folder = new File(folderName);
		File[] listOfFiles = folder.listFiles();
		
		if(listOfFiles==null)
		{
			return files;
		}

		for (File file : listOfFiles)
		{
		    if (file.isFile())
		    {
		    	files.add(file);
		    }
		}
		
		return files;
	}

	/**
	 * Merge currentState into state if previousState and currentState contain differences
	 * @param previousState
	 * @param currentState
	 * @param state
	 */
	private void mergeStates(AppState previousState, AppState currentState, AppState state)
	{
		AppState previousStateTree=previousState;
		AppState stateTree=state;
		
		try
		{
			mergeState(previousState, currentState, state);
			
			Path lastPath=currentState.getCurrentPath();
			if(lastPath==null || lastPath.getWidgets().size()==0)
			{
				// No path or empty
				return;
			}
			
			// Start a new path
			StateController.startNewPath(state, lastPath.getProductVersion(), lastPath.getTester());
			Path newPath=state.getCurrentPath();
			
			List<PathStep> pathSteps=lastPath.getPathSteps();
			for(PathStep pathStep:pathSteps)
			{
				Widget currentStateNextWidget=pathStep.getAction();
				Widget previousStateNextWidget=null;
				if(previousState!=null)
				{
					previousStateNextWidget=previousState.geVisibleWidget(currentStateNextWidget.getId());
				}
				Widget stateNextWidget=null;
				if(state!=null)
				{
					stateNextWidget=state.geVisibleWidget(currentStateNextWidget.getId());
				}
				
				if(previousStateNextWidget==null && stateNextWidget==null)
				{
					// Widget has been added by local user and not already added during the merge - insert a new widget
					previousState=null;
					currentState=currentStateNextWidget.getNextState();
					Widget newWidget=new Widget(currentStateNextWidget);
					newWidget.setId(currentStateNextWidget.getId());
					AppState nextState=null;
					if(currentState.isBookmark())
					{
						// On a bookmark - sync other states
						previousState=previousStateTree.findStateFromBookmark(currentState.getBookmark());
						nextState=stateTree.findStateFromBookmark(currentState.getBookmark());
					}
					state=StateController.insertWidget(state, newWidget, nextState, lastPath.getProductVersion(), lastPath.getTester(), newPath);
					mergeState(previousState, currentState, state);
					state.addProductVersion(lastPath.getProductVersion());
				}
				else if(previousStateNextWidget!=null && stateNextWidget==null)
				{
					// Widget has been removed by external user - already removed
					previousState=previousStateNextWidget.getNextState();
					currentState=currentStateNextWidget.getNextState();
					state=null;
				}
				else if(previousStateNextWidget!=null && stateNextWidget!=null)
				{
					// Widget existed - perform again
					previousState=previousStateNextWidget.getNextState();
					currentState=currentStateNextWidget.getNextState();
					state=StateController.performWidget(state, stateNextWidget, newPath);
					mergeState(previousState, currentState, state);
					state.addProductVersion(lastPath.getProductVersion());
				}
				else if(previousStateNextWidget==null && stateNextWidget!=null)
				{
					// Widget has been added by local user and has been added during the merge
					previousState=null;
					currentState=currentStateNextWidget.getNextState();
					state=StateController.performWidget(state, stateNextWidget, newPath);
					mergeState(previousState, currentState, state);
					state.addProductVersion(lastPath.getProductVersion());
				}
			}
		}
		catch(Exception e)
		{
			return;
		}
	}

	/**
	 * Merge currentState into state if previousState and currentState contain differences
	 * @param previousState
	 * @param currentState
	 * @param state
	 */
	private void mergeState(AppState previousState, AppState currentState, AppState state)
	{
		List<Widget> currentStateWidgets=currentState.getVisibleWidgets();
		for(Widget currentStateWidget:currentStateWidgets)
		{
			Widget previousStateWidget=null;
			if(previousState!=null)
			{
				previousStateWidget=previousState.getWidget(currentStateWidget.getId());
			}
			if(previousStateWidget==null)
			{
				// Added widget
				if(currentStateWidget.getWidgetType()!=WidgetType.ACTION)
				{
					// Not an action - add
					Widget newWidget=new Widget(currentStateWidget);
					state.addWidget(newWidget);
				}
			}
			else
			{
				// Not added
				Widget stateWidget=state.getWidget(currentStateWidget.getId());
				if(stateWidget==null)
				{
					// Has been removed externally
				}
				else
				{
					// Exists in external model - check if it has been changed and needs to be updated
					if(previousStateWidget.getLocationArea()!=null && !previousStateWidget.getLocationArea().equals(currentStateWidget.getLocationArea()))
					{
						// Location or size change
						stateWidget.setLocationArea(currentStateWidget.getLocationArea());
					}
					if(previousStateWidget.getValidExpression()!=null && !previousStateWidget.getValidExpression().equals(currentStateWidget.getValidExpression()))
					{
						// Valid expression has changed
						stateWidget.setValidExpression(currentStateWidget.getValidExpression());
					}
					if(previousStateWidget.getText()!=null && !previousStateWidget.getText().equals(currentStateWidget.getText()))
					{
						// Text has changed
						stateWidget.setText(currentStateWidget.getText());
					}
					List<String> keys=previousStateWidget.getMetadataKeys();
					for(String key:keys)
					{
						if(!key.equals("matching_widget"))
						{
							Object value=previousStateWidget.getMetadata(key);
							if(value!=null)
							{
								Object currentValue=currentStateWidget.getMetadata(key);
								if(currentValue==null)
								{
									// Metadata for key has been removed
									stateWidget.removeMetadata(key);
								}
								else if(!value.equals(currentValue))
								{
									// Metadata value has changed for key
									stateWidget.putMetadata(key, value);
								}
							}
						}
					}
					List<String> currentKeys=currentStateWidget.getMetadataKeys();
					for(String currentKey:currentKeys)
					{
						Object value=previousStateWidget.getMetadata(currentKey);
						if(value!=null)
						{
							Object previousValue=previousStateWidget.getMetadata(currentKey);
							if(previousValue==null)
							{
								// New metadata value
								stateWidget.putMetadata(currentKey, value);
							}
						}
					}
				}
			}
		}

		if(previousState!=null)
		{
			List<Widget> previousStateWidgets=previousState.getVisibleWidgets();
			for(Widget previousStateWidget:previousStateWidgets)
			{
				Widget currentStateWidget=currentState.getWidget(previousStateWidget.getId());
				if(currentStateWidget==null)
				{
					// Widget has been removed
					state.removeWidget(previousStateWidget.getId());
				}
			}
		}
	}
}
