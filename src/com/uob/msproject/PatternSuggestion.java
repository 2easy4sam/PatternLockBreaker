package com.uob.msproject;

import java.util.ArrayList;

/**
 * This class contains methods for suggesting patterns once the nodes have been
 * detected
 */
public class PatternSuggestion {
	// The list of all possible patterns
	public static ArrayList<String> sPatterns = new ArrayList<String>();
	// The list of all training patterns
	// this set is never reloaded in cross validation
	public static ArrayList<String> sTrainingSet = new ArrayList<String>();

	// Markov probabilities
	public static float[] sStartingProbabilities = new float[9];
	public static float[][] sBigramProbabilities = new float[9][9];
	public static float[][][] sTrigramProbabilities = new float[9][9][9];

	// the number of iterations each HC algorithm will be executed
	public static int sIter = 0;

	private Node[] m1dTo2dMap;
	private boolean[][] mPattern;

	private final static int INVALID_NEXT_NODE = -1;

	/**
	 * Constructor
	 */
	public PatternSuggestion() {
		mPattern = new boolean[3][3];
		m1dTo2dMap = new Node[9];
		for (int i = 0; i < 9; i++) {
			m1dTo2dMap[i] = new Node(i);
		}
	}

	/**
	 * Given a set of patterns, try to work out the actual pattern
	 * 
	 * @param nodes
	 *           : the nodes are always arranged in ascending order
	 */
	public String guessPattern(String nodes) {
		StringBuilder pattern = new StringBuilder();
		ArrayList<Integer> nodeList = getNodeList(nodes);
		// a list of invalid nodes
		ArrayList<Integer> invalidNodeList = new ArrayList<Integer>();
		ArrayList<Integer> sequence = new ArrayList<Integer>(2);

		reinitNodeStatus();

		mainloop: for (int i = 0; i < nodes.length(); i++) {
			int nextNode = -1;
			while (nextNode == INVALID_NEXT_NODE) {
				nextNode = roulette(nodeList, sequence);
				// there is only 1 node left in the list and it's inaccessible
				if (nextNode == INVALID_NEXT_NODE)
					break mainloop;
			}

			sequence = addToSequence(nextNode, sequence);
			// remove the next node from the node list
			nodeList.remove(Integer.valueOf(nextNode));
			// append the guessed node
			pattern.append(nextNode);

			// check if any of the remaining nodes is accessible from the
			// last node in the sequence
			if (sequence.size() > 1) {
				invalidNodeList.clear();
				for (int j = 0; j < nodeList.size(); j++) {
					if (!isNextNodeValid(nextNode, nodeList.get(j)))
						invalidNodeList.add(nodeList.get(j));
				}

				// TODO: check if this causes stack overflow
				if (!invalidNodeList.isEmpty() && invalidNodeList.size() == nodeList.size()) {
					break;
				}
			}
		}

		return mutate(0.013, pattern.toString());
	}

	/**
	 * TODO: to be tested Loop through the entire list of possible patterns and
	 * determine the most likely one
	 */
	public String guessPattern2(String nodes) {
		StringBuilder solution = new StringBuilder();
		ArrayList<Integer> nodeList = getNodeList(nodes);
		ArrayList<String> candidateList = new ArrayList<String>();

		// loop through the list
		for (int i = 0; i < sPatterns.size(); i++) {
			// check if the pattern contains all the nodes
			ArrayList<Integer> pattern = getNodeList(sPatterns.get(i));
			if (pattern.size() == nodeList.size()) {
				for (int j = 0; j < nodeList.size(); j++) {
					// stop if any node is not found
					if (!pattern.contains(Integer.valueOf(nodeList.get(j)))) {
						break;
					}
					candidateList.add(sPatterns.get(i));
				}
				// pattern contains all nodes, add to candidate list
			}
		} // end for

		ArrayList<Integer> sequence = new ArrayList<Integer>();

		int i = 0;
		// now, find the pattern that has the highest probability
		while (nodeList.size() != 0) {
			int nextNode = -1;
			while (nextNode == INVALID_NEXT_NODE) {
				nextNode = roulette(nodeList, sequence);
			}

			ArrayList<String> candidateList2 = new ArrayList<String>();
			// find all patterns whose ith node is this node
			for (int j = 0; j < candidateList.size(); j++) {
				if (Utility.charToInt(candidateList.get(j).charAt(i)) == nextNode) {
					candidateList2.add(candidateList.get(j));
				}
			}

			// if none of the candidate's ith node is nextNode
			// reselect
			if (candidateList2.size() == 0) {
				continue;
			} else {
				candidateList = new ArrayList<String>(candidateList2);
				i++;
			}

			sequence = addToSequence(nextNode, sequence);
			// remove the next node from the node list
			nodeList.remove(Integer.valueOf(nextNode));
			// append the guessed node
			solution.append(nextNode);
		}

		return solution.toString();
	}

	private ArrayList<Integer> addToSequence(int node, ArrayList<Integer> sequence) {
		int len = sequence.size();

		switch (len) {
		case 0:
		case 1:
			sequence.add(node);
			break;
		case 2:
			sequence.remove(0);
			sequence.add(node);
			break;
		}

		return sequence;
	}

	/**
	 * The function calls itself recursively until a valid node is selected
	 * 
	 * @param sequence
	 *           : the sequence of nodes, the size of which is used to determine
	 *           from which variable to retrieve the probability. When working
	 *           out the starting node, this sequence is empty
	 */
	private int roulette(ArrayList<Integer> nodeList, ArrayList<Integer> sequence) {
		float rouletteLen = 0;
		int nextNode = -1;

		for (int i = 0; i < nodeList.size(); i++) {
			rouletteLen += getProbability(nodeList.get(i), sequence);
		}

		float stop = Utility.randFloat(0, rouletteLen);
		for (int i = 0; i < nodeList.size(); i++) {
			float lowerBound = 0;
			float upperBound = 0;
			// add the fitness up
			for (int j = i; j >= 0; j--) {
				upperBound += getProbability(nodeList.get(j), sequence);
			}

			lowerBound = upperBound - getProbability(nodeList.get(i), sequence);

			if (stop >= lowerBound && stop <= upperBound) {
				nextNode = nodeList.get(i);

				if (!sequence.isEmpty()) {
					if (!isNextNodeValid(sequence.get(sequence.size() - 1), nextNode)) {
						return INVALID_NEXT_NODE;
					}
				}

				// mark as visited
				updateNodeStatus(nextNode, true);
				break;
			}
		}

		return nextNode;
	}

	/**
	 * Mutate a pattern
	 * 
	 * @param mr
	 *           : the mutation rate
	 */
	private String mutate(double mr, String pattern) {
		// only mutate if the generated number
		// is below the threshold mutation rate
		if (Utility.randFloat(0, 1) <= mr)
			return swap(pattern);
		return pattern;
	}

	/**
	 * The swap mutation operator
	 * 
	 * @param pattern
	 *           : the pattern in which the mutation takes place
	 */
	private String swap(String pattern) {
		StringBuilder newPattern = new StringBuilder();
		ArrayList<Integer> nodeList = getNodeList(pattern);
		int a = 0, b = 0;
		int temp, temp2;
		boolean valid = false;

		try {
			while (a == b) {
				a = Utility.randInt(0, pattern.length() - 1);
				b = Utility.randInt(0, pattern.length() - 1);
			}

			temp = nodeList.get(a);
			temp2 = nodeList.get(b);
			nodeList.set(a, temp2);
			nodeList.set(b, temp);

			for (int i = 0; i < nodeList.size(); i++) {
				newPattern.append(nodeList.get(i));
			}
			// TODO: check if the new pattern is valid
			valid = isPatternValid(newPattern.toString(), pattern.length());
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}

		return valid ? newPattern.toString() : pattern;
	}

	/**
	 * Get the probability from the corresponding data structure
	 */
	private float getProbability(int nextNode, ArrayList<Integer> sequence) {
		int size = sequence.size();
		float probability = 0;

		try {
			switch (size) {
			case 0:
				probability += sStartingProbabilities[nextNode];
				break;
			case 1:
				probability += sBigramProbabilities[sequence.get(0)][nextNode];
				break;
			case 2:
				probability += sTrigramProbabilities[sequence.get(0)][sequence.get(1)][nextNode];
				break;
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
		} catch (IndexOutOfBoundsException e) {
			e.printStackTrace();
		}

		return probability;
	}

	/**
	 * Check if the next node is directly reachable from the current node
	 * 
	 * @param currentNode
	 *           : the current node
	 * @param nextNode
	 *           : the anticipated next node
	 */
	private boolean isNextNodeValid(int currentNode, int nextNode) {
		try {
			Node current = m1dTo2dMap[currentNode];
			Node next = m1dTo2dMap[nextNode];

			int minX = Math.min(current.x, next.x);
			int minY = Math.min(current.y, next.y);

			int xDiff = Math.abs(current.x - next.x);
			int yDiff = Math.abs(current.y - next.y);

			int lookupX = -1;
			int lookupY = -1;

			lookupX = xDiff == 2 ? minX + 1 : next.x;
			lookupY = yDiff == 2 ? minY + 1 : next.y;

			if ((xDiff == 2 && yDiff == 0) || (xDiff == 0 && yDiff == 2) || (xDiff == 2 && yDiff == 2))
				return mPattern[lookupX][lookupY];
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
	}

	/**
	 * Reinitialise the status of all nodes
	 */
	private void reinitNodeStatus() {
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				mPattern[i][j] = false;
			}
		}
	}

	/**
	 * Change the status of a node
	 */
	private void updateNodeStatus(int nodeNum, boolean status) {
		Node node = m1dTo2dMap[nodeNum];
		mPattern[node.x][node.y] = true;
	}

	/**
	 * Check if a pattern is valid
	 * 
	 * @param length
	 *           : the length of the actual pattern
	 */
	private boolean isPatternValid(String pattern, int length) {
		// a pattern is meant to consist of a minimum of 4 nodes
		if (pattern.length() != length || pattern.length() < 4) {
			return false;
		}
		boolean isValid = true;
		ArrayList<Integer> nodeList = getNodeList(pattern);

		// reinitialise node status
		reinitNodeStatus();

		for (int i = 0; i < nodeList.size() - 1; i++) {
			int node1 = nodeList.get(i);
			int node2 = nodeList.get(i + 1);
			updateNodeStatus(node1, true);

			isValid &= isNextNodeValid(node1, node2);
			if (!isValid)
				break;
			else
				updateNodeStatus(node2, true);
		}

		return isValid;
	}

	/**
	 * The random mutation hill climbing algorithm that aims to find the solution
	 * with the highest fitness. It keeps finding a better solution
	 * 
	 * @param iter
	 *           : the number of iterations to run
	 * @param nodes
	 *           : the detected nodes
	 */
	public String randomMutationHC(int iter, String nodes) {
		String bestSol = "";

		// keep calling the function until a valid pattern is generated
		do {
			bestSol = guessPattern(nodes);
		} while (!isPatternValid(bestSol, nodes.length()));

		for (int i = 0; i < iter; i++) {
			String newSolution = swap(bestSol);
			if (fitnessFunc(newSolution) > fitnessFunc(bestSol))
				bestSol = newSolution;
		}

		return bestSol;
	}

	/**
	 * The random restart hill climbing method mutation takes place at a certain
	 * rate. It searches for a local optimum
	 */
	public String randomRestartHC(int iter, int iter2, String nodes) {
		String besSol = "";
		float bestFitness = 0;
		for (int i = 0; i < iter; i++) {
			// generate a new pattern each iteration
			String currentSol = guessPattern(nodes);

			for (int j = 0; j < iter2; j++) {
				float oldFitness = fitnessFunc(currentSol);
				// String newSol = swap(currentSol);
				String newSol = guessPattern(nodes); // TODO: remove later

				float newFitness = fitnessFunc(newSol);
				if (newFitness > oldFitness) {
					currentSol = new String(newSol);
				}
			}

			if (fitnessFunc(currentSol) > bestFitness) {
				besSol = currentSol;
			}
		}

		return besSol;
	}

	/**
	 * The stochastic HC algorithm
	 * 
	 * @param iter
	 *           : the total number of iterations
	 * @param t
	 *           : a parameter that determines the acceptance rate; should be set
	 *           to 25 usually
	 */
	public String stochasticHC(int iter, int t, String nodes) {
		String bestSol = guessPattern(nodes);

		for (int i = 0; i < iter; i++) {
			String newSol = swap(bestSol);
			float newFitness = fitnessFunc(newSol);
			float bestFitness = fitnessFunc(bestSol);
			if (newFitness > (1 / (1 + Math.exp((newFitness - bestFitness) / t)))) {
				bestSol = newSol;
			}
		}

		return bestSol;
	}

	/**
	 * The simulated annealing algorithm
	 * 
	 * @param sTemp
	 *           : the starting temperature
	 * @param cr
	 *           : the cooling rate
	 * @param iter
	 *           : the number of iterations
	 * @param nodes
	 *           : the detected nodes
	 */
	public String simulatedAnnealing(double sTemp, double cr, int iter, String nodes) {
		float temperature = (float) sTemp;
		// the best solution
		String bestSol = guessPattern(nodes);

		for (int i = 0; i < iter; i++) {
			float oldFitness = fitnessFunc(bestSol);
			String newSol = swap(bestSol);
			float newFitness = fitnessFunc(newSol);

			if (newFitness > oldFitness) {
				if (!(acceptanceFunc(oldFitness, newFitness, temperature) < Utility.randFloat(0, 1))) {
					bestSol = newSol;
				}
			} else {
				bestSol = newSol;
			}

			// cool down the temperature
			temperature *= cr;
		}

		return bestSol;
	}

	/**
	 * The acceptance function that
	 */
	private float acceptanceFunc(float oldFitness, float newFitness, float temperature) {
		return (float) Math.exp(-(Math.abs(newFitness - oldFitness)) / temperature);
	}

	/**
	 * Extract the nodes from a string and store them in a list
	 */
	private ArrayList<Integer> getNodeList(String nodes) {
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < nodes.length(); i++) {
			if (Utility.charToInt(nodes.charAt(i)) == -1) {
				System.out.println("error");
			} else {
				list.add(Utility.charToInt(nodes.charAt(i)));
			}
		}

		return list;
	}

	/**
	 * The fitness function that measures the fitness of a solution/pattern in
	 * this case, we are trying to find the solution with the highest possibility
	 * TODO: to be tested
	 */
	private float fitnessFunc(String pattern) {
		float fitness = 0;
		ArrayList<Integer> sequence = new ArrayList<Integer>();
		ArrayList<Integer> nodeList = getNodeList(pattern);

		for (int i = 0; i < pattern.length(); i++) {
			fitness += getProbability(nodeList.get(i), sequence);
			sequence = addToSequence(nodeList.get(i), sequence);
		}

		return fitness;
	}

	/**
	 * This struct represents a node
	 */
	private class Node {
		public int x, y;

		/**
		 * The default constructor
		 */
		public Node() {

		}

		public Node(int node) {
			switch (node) {
			case 0:
				x = 0;
				y = 0;
				break;
			case 1:
				x = 0;
				y = 1;
				break;
			case 2:
				x = 0;
				y = 2;
				break;
			case 3:
				x = 1;
				y = 0;
				break;
			case 4:
				x = 1;
				y = 1;
				break;
			case 5:
				x = 1;
				y = 2;
				break;
			case 6:
				x = 2;
				y = 0;
				break;
			case 7:
				x = 2;
				y = 1;
				break;
			case 8:
				x = 2;
				y = 2;
				break;
			}
		}
	}
}
