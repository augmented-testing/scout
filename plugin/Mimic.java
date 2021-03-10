package plugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import scout.AppState;
import scout.StateController;

public class Mimic
{
	private static final int POPULATION = 100;
	private static final int THINK_TIME = 5000;
	private static final int TIME_INCREASE_COMPLEXITY = 2000;
	private static final int MAX_SCORE = 1000;
	private static final int NO_SOLUTIONS = 5;

	private AppState currentState = null;
	private Random rand = new Random(System.currentTimeMillis());
	private long startTime = 0;

	public Mimic(AppState currentState)
	{
		this.currentState = currentState;
	}

	/**
	 * Find the best model
	 * @param params Parameters for the predicted model
	 * @param textParams Parameters for all the examples to learn from
	 * @param isSimpleType true to use a simple comparison based on text length
	 * @return The best model before running out of time or null
	 */
	public DynamicPart findModel(List<KeyValue> params, List<TextParameters> textParams, boolean isSimpleType)
	{
		try
		{
			startTime = System.currentTimeMillis();
			List<DynamicPart> bestParts = new ArrayList<DynamicPart>();
			int maxScore = MAX_SCORE * textParams.size();
			int noNested = 1;

			// Create an initial population
			for (int i = 0; i < POPULATION; i++)
			{
				DynamicPart part = new DynamicPart();
				assignRandomParts(part, params, noNested);
				part.setComplexity(part.toString().length());
				bestParts.add(part);

				// Determine the score (fitnesse)
				int partScore = 0;
				for (TextParameters textParam : textParams)
				{
					List<KeyValue> cloneParams = textParam.getParams();
					String actualText = textParam.getText();
					String predictedText = part.getText(cloneParams);
					int score = getScore(actualText, predictedText, MAX_SCORE, isSimpleType);
					partScore += score;
				}
				part.setScore(partScore);
			}

			// Iterate to find the best model until out of time
			while (System.currentTimeMillis() < startTime + THINK_TIME)
			{
				if (currentState != StateController.getCurrentState())
				{
					// Have moved on to another state - abort
					return null;
				}

				// Adjust model complexity
				if (noNested == 1 && System.currentTimeMillis() > startTime + TIME_INCREASE_COMPLEXITY)
				{
					noNested = 2;
				}

				// Create mutants
				List<DynamicPart> mutantParts = new ArrayList<DynamicPart>();
				for (DynamicPart part : bestParts)
				{
					// Create a mutant
					DynamicPart mutant = new DynamicPart(part);
					mutateRandomPart(mutant, params, noNested);
					mutant.setComplexity(mutant.toString().length());

					// Add the mutant first or last
					if(rand.nextInt(2)==0)
					{
						mutantParts.add(0, mutant);
					}
					else
					{
						mutantParts.add(mutant);
					}

					// Determine the score (fitnesse)
					int partScore = 0;
					for (TextParameters textParam : textParams)
					{
						List<KeyValue> cloneParams = textParam.getParams();
						String actualText = textParam.getText();
						String predictedText = mutant.getText(cloneParams);
						int score = getScore(actualText, predictedText, MAX_SCORE, isSimpleType);
						partScore += score;
					}
					mutant.setScore(partScore);
				}
				bestParts.addAll(mutantParts);

				// Sort the parts
				Collections.sort(bestParts);

				// Count the no of solutions
				int noSolutions = 0;
				for (DynamicPart part : bestParts)
				{
					if (part.getScore() == maxScore)
					{
						noSolutions++;
					}
				}
				if(noSolutions >= NO_SOLUTIONS)
				{
					DynamicPart bestPart = bestParts.get(0);
					return bestPart;
				}
				
				// Remove half of the population
				List<DynamicPart> remainingParts = new ArrayList<DynamicPart>();
				int noRemaining = bestParts.size() / 2;
				for (int j = 0; j < noRemaining; j++)
				{
					remainingParts.add(bestParts.get(j));
				}
				bestParts = remainingParts;
			}

			if (bestParts.size() == 0)
			{
				// No model found
				return null;
			}

			// Get the best part
			DynamicPart bestPart = bestParts.get(0);
			if (bestPart.getScore() < maxScore)
			{
				// Not a perfect score
				return null;
			}

			return bestPart;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Determine if the model is valid for all clones
	 * @param part The model to validate
	 * @param textParams Parameters for all examples
	 * @return true if valid or false if not
	 */
	public boolean isModelValid(DynamicPart part, List<TextParameters> textParams)
	{
		for (TextParameters textParam : textParams)
		{
			String text = part.getText(textParam.getParams());
			if (!textParam.getText().equals(text))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Create a model of random parts
	 * @param part The part to replace
	 * @param params All available parameters to select from
	 * @param noNestedFunctions No remaining of possibly nested functions (0 uses a parameter)
	 */
	private void assignRandomParts(DynamicPart part, List<KeyValue> params, int noNestedFunctions)
	{
		// Reset the part
		part.setParameters(new ArrayList<DynamicPart>());

		// Select to use a command or a parameter by random
		boolean useCommand = noNestedFunctions > 0 && rand.nextInt(2) == 0;
		if (useCommand)
		{
			// Select a command by random
			int commandNo = rand.nextInt(DynamicPart.commands.length);
			part.setFunctionName(DynamicPart.commands[commandNo]);
			part.setNoParameters(DynamicPart.noParams[commandNo]);
			for (int i = 0; i < part.getNoParameters(); i++)
			{
				DynamicPart dynamicPart = new DynamicPart();
				part.getParameters().add(dynamicPart);
				assignRandomParts(dynamicPart, params, noNestedFunctions - 1);
			}
		} else
		{
			// Select a parameter by random
			int paramNo = rand.nextInt(params.size());
			part.setFunctionName(params.get(paramNo).getKey());
			part.setNoParameters(0);
		}
	}

	/**
	 * Mutate one randomly selected part
	 * @param part The root of the nested expression
	 * @param params All available parameters to select from
	 * @param noNestedFunctions No remaining nested functions (0 uses a parameter)
	 */
	private void mutateRandomPart(DynamicPart part, List<KeyValue> params, int noNestedFunctions)
	{
		List<DynamicPart> parts = new ArrayList<DynamicPart>();
		getAllParts(part, parts);
		int randomPartNo = rand.nextInt(parts.size());
		DynamicPart randomPart = parts.get(randomPartNo);
		assignRandomParts(randomPart, params, noNestedFunctions);
	}

	private void getAllParts(DynamicPart part, List<DynamicPart> parts)
	{
		parts.add(part);
		List<DynamicPart> partParams = part.getParameters();
		for (DynamicPart partParam : partParams)
		{
			getAllParts(partParam, parts);
		}
	}

	/**
	 * Return a score between 0 and maxScore depending on the similarity between s1 and s2.
	 * @param s1
	 * @param s2
	 * @param maxScore
	 * @param isSimpleType
	 * @return A similarity score between 0 and maxScore
	 */
	private static int getScore(String s1, String s2, int maxScore, boolean isSimpleType)
	{
		if (s1.equalsIgnoreCase(s2))
		{
			return maxScore;
		}

		if (isSimpleType)
		{
			int diff = Math.abs(s1.length() - s2.length());
			int score = Math.max(maxScore - diff, 0) - 1;
			return score;
		}

		int editDistance = 0;
		if (s1.length() < s2.length())
		{
			String swap = s1;
			s1 = s2;
			s2 = swap;
		}
		int bigLen = s1.length();
		editDistance = computeDistance(s1, s2);
		if (bigLen == 0)
		{
			return maxScore;
		} else
		{
			return (bigLen - editDistance) * maxScore / bigLen;
		}
	}

	/**
	 * Compute the Levenshtein distance between two strings
	 * @param s1
	 * @param s2
	 * @return The distance between s1 and s2
	 */
	private static int computeDistance(String s1, String s2)
	{
		s1 = s1.toLowerCase();
		s2 = s2.toLowerCase();

		int[] costs = new int[s2.length() + 1];
		for (int i = 0; i <= s1.length(); i++)
		{
			int lastValue = i;
			for (int j = 0; j <= s2.length(); j++)
			{
				if (i == 0)
				{
					costs[j] = j;
				} else
				{
					if (j > 0)
					{
						int newValue = costs[j - 1];
						if (s1.charAt(i - 1) != s2.charAt(j - 1))
						{
							newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
						}
						costs[j - 1] = lastValue;
						lastValue = newValue;
					}
				}
			}
			if (i > 0)
			{
				costs[s2.length()] = lastValue;
			}
		}
		return costs[s2.length()];
	}
}
