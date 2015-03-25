import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Scanner;


/**
* Recommender. Recommends ratings to movies based on collaborative-filtering (user-based and item-based)
* @Author Andrew Elenbogen and Quang Tran
* @Version March 9, 2015
*/

public class Recommender 
{
	private static final String TRAINING_DATA_LOCATION = "/tmp/ua.base";
	private static final String TEST_DATA_LOCATON = "/tmp/ua.test";
	private static final String MOVIE_NAME_DATA_LOCATION="/tmp/u.item";
	
	private HashMap<Integer, HashMap<Integer, Float>> userToMovieToRating;
	private HashMap<Integer, HashMap<Integer, Float>> movieToUserToRating;
	private HashMap<Integer, HashMap<Integer, Integer>> testData;
	private HashMap<Integer, Float> userIdToAverage;
	private HashMap<Integer, String> movieIdToName;
	
	public Recommender()
	{
		userToMovieToRating=new  HashMap<Integer, HashMap<Integer, Float>>();
		movieToUserToRating=new  HashMap<Integer, HashMap<Integer, Float>>();
		testData=new  HashMap<Integer, HashMap<Integer, Integer>>();
		userIdToAverage=new HashMap<Integer, Float>();
		movieIdToName= new HashMap<Integer, String>();
		readFile();
		subtractAverageFromUsers();
	}
	
	/*
	 * This method normalizes all the ratings based on each user's average rating.
	 */
	private void subtractAverageFromUsers()
	{	
		for(Integer userId: userToMovieToRating.keySet())
		{
			float sum=0;
			for(Float rating: userToMovieToRating.get(userId).values())
			{
				sum+=rating;
			}
			userIdToAverage.put(userId, sum/userToMovieToRating.get(userId).values().size());
		}
		for(Integer userId: userToMovieToRating.keySet())
		{
			for(Integer movieId: userToMovieToRating.get(userId).keySet())
			{
				userToMovieToRating.get(userId).put(movieId, userToMovieToRating.get(userId).get(movieId)
						-userIdToAverage.get(userId));
				movieToUserToRating.get(movieId).put(userId, movieToUserToRating.get(movieId).get(userId)
						-userIdToAverage.get(userId));
			}
		}
	}
	
	/*
	 * This method de-normalizes our data.
	 */
	private void fixAverage(HashMap<Integer, HashMap<Integer, Float>> userToMovieToRating, boolean userIsfirstKey)
	{
		for(Integer firstKey: userToMovieToRating.keySet())
		{
			for(Integer secondKey: userToMovieToRating.get(firstKey).keySet())
			{
				float toAdd;
				if(userIsfirstKey)
				{
					toAdd=userIdToAverage.get(firstKey);
				}
				else
				{
					toAdd=userIdToAverage.get(secondKey);
				}
				
				
				userToMovieToRating.get(firstKey).put(secondKey, userToMovieToRating.get(firstKey).get(secondKey)
						+toAdd);
			}
		}
	}
	
	
	/*
	 * Takes the data file and turns it into HashMap of HashMaps
	 */
	private void readFile()
	{
		try(Scanner scanner=new Scanner(new File(TRAINING_DATA_LOCATION)))
		{
			while(scanner.hasNextLine())
			{
				String[] split=scanner.nextLine().split("\t");
				
				int[] splitInts=new int[split.length];
				for(int i=0; i<split.length; i++)
				{
					splitInts[i]=Integer.parseInt(split[i]);
				}
				if(userToMovieToRating.get(splitInts[0])==null)
				{
					userToMovieToRating.put(splitInts[0], new HashMap<Integer, Float>());
				}
				
				userToMovieToRating.get(splitInts[0]).put(splitInts[1], (float) splitInts[2]);
				
				if(movieToUserToRating.get(splitInts[1])==null)
				{
					movieToUserToRating.put(splitInts[1], new HashMap<Integer, Float>());
				}
				
				movieToUserToRating.get(splitInts[1]).put(splitInts[0], (float) splitInts[2]);
			}
		}
		catch (IOException e){
			System.out.println(e);
			System.exit(0);
		}
		
		try(Scanner scanner=new Scanner(new File(TEST_DATA_LOCATON)))
		{
			while(scanner.hasNextLine())
			{
				String[] split=scanner.nextLine().split("\t");
				
				int[] splitInts=new int[split.length];
				for(int i=0; i<split.length; i++)
				{
					splitInts[i]=Integer.parseInt(split[i]);
				}
				if(testData.get(splitInts[0])==null)
				{
					testData.put(splitInts[0], new HashMap<Integer, Integer>());
				}
				
				testData.get(splitInts[0]).put(splitInts[1], splitInts[2]);
			}
		}
		catch (IOException e){
			System.out.println(e);
			System.exit(0);
		}
		
		try(Scanner scanner=new Scanner(new File(MOVIE_NAME_DATA_LOCATION)))
		{
			while(scanner.hasNextLine())
			{
				String[] split=scanner.nextLine().split("\\|");
				//System.out.println(split[0]+" "+split[1]);
				movieIdToName.put(Integer.parseInt(split[0]), split[1]);
			}
		}
		catch (IOException e){
			System.out.println(e);
			System.exit(0);
		}
		
		try(BufferedReader reader=new BufferedReader(new FileReader(new File(MOVIE_NAME_DATA_LOCATION))))
		{
			String curLine=null;
			while( (curLine=reader.readLine() )!=null)
			{
				String[] split=curLine.split("\\|");
				movieIdToName.put(Integer.parseInt(split[0]), split[1]);
			}
		}
		catch (IOException e){
			System.out.println(e);
			System.exit(0);
		}
	}
	
	/*
	 * This method takes k and returns the average rating of k closest neighbors. 
	 */
	public Float getAverageOfClosestNeighbors(int firstKey, int secondKey, int k, HashMap<Integer, HashMap<Integer, Float>> map, int threshold)
	{
		if(map.get(firstKey)==null)
		{
			return null;
		}
		
		PriorityQueue<Integer> queue= new PriorityQueue<Integer>( new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) 
			{		
				double distanceDiff=getDistance(map.get(firstKey), map.get(o2))
						-getDistance(map.get(firstKey), map.get(o1));
				if(distanceDiff<0)
					return (int) Math.floor(distanceDiff);
				else
					return (int) Math.ceil(distanceDiff);
			}
		});
		queue.addAll(map.keySet());
		
		
		ArrayList<Integer> nearest=new ArrayList<Integer>();
		while(nearest.size()<k)
		{
			if(queue.isEmpty())
			{
				break;
			}
			int currentUserId=queue.remove();
			int keysInCommon=0;
			for(int key: map.get(currentUserId).keySet())
			{
				if(map.get(firstKey).containsKey(key))
				{
					keysInCommon++;
				}
			}
			if(map.get(currentUserId).get(secondKey) !=null && keysInCommon>=threshold) //TO DO: Use Threshold
				nearest.add(currentUserId);
		}
		float sum=0;
		for(int current: nearest)
		{
			sum+=map.get(current).get(secondKey);
		}
		if(nearest.isEmpty())
		{
			return null;
		}
		
		return sum/nearest.size();
	}
	
	/*
	 * Returning distance from one item to another (either user or movie)
	 */
	public double getDistance(HashMap<Integer, Float> map1, HashMap<Integer, Float> map2)
	{
		double total=0;
		for(int id: map1.keySet())
		{
			if(map2.keySet().contains(id))
			{
				total+=map1.get(id)*map2.get(id);
			}
		}
		double map1Total=0;
		for (float values: map1.values()){
			map1Total += Math.pow(values, 2);
		}
		double map2Total=0;
		for (float values: map2.values()){
			map2Total += Math.pow(values, 2);
		}
		
		
		return total/(Math.sqrt(map1Total) * Math.sqrt(map2Total));
	}
	
	/*
	 * User-based recommendation system
	 */
	public HashMap<Integer, HashMap<Integer, Float>> reccomendByUser(int k, int threshold)
	{
		HashMap<Integer, HashMap<Integer, Float>> reccomendations= new HashMap<Integer, HashMap<Integer, Float>>();
		
		for(int currentUserId: testData.keySet())
		{
			for(int movieId: testData.get(currentUserId).keySet())
			{	
				if(reccomendations.get(currentUserId)==null)
					reccomendations.put(currentUserId, new HashMap<Integer, Float>());
				Float averageValue = getAverageOfClosestNeighbors(currentUserId, movieId, k, userToMovieToRating, threshold);
				if (averageValue == null){
					averageValue = 0f;
				}
				reccomendations.get(currentUserId).put(movieId, averageValue);
				//System.out.println("Reccomendation for "+currentUserId+" "+movieId+" "+averageValue);
			}
		}		
		return reccomendations;
	}
	
	/*
	 * Item-based recommendation system
	 */
	public HashMap<Integer, HashMap<Integer, Float>> reccomendByItem(int k, int threshold)
	{
		HashMap<Integer, HashMap<Integer, Float>> recomendations= new HashMap<Integer, HashMap<Integer, Float>>();
		
		for(int currentUserId: testData.keySet())
		{
			for(int movieId: testData.get(currentUserId).keySet())
			{	
				if(recomendations.get(currentUserId)==null)
					recomendations.put(currentUserId, new HashMap<Integer, Float>());
				Float averageValue=getAverageOfClosestNeighbors(movieId, currentUserId, k, movieToUserToRating, threshold);
				if(averageValue==null)
				{
					averageValue=0f;
				}
				recomendations.get(currentUserId).put(movieId, averageValue);
				//System.out.println("Reccomendation for "+currentUserId+" "+movieId+" "+averageValue);
			}
		}
		
		return recomendations;
	}
	
	/*
	 * Takes the HashMap of our recommendation and the HashMap of the actual data, calculates
	 * the average differences for each movie
	 */
	public HashMap<Integer, Float> accuracy(HashMap<Integer, HashMap<Integer, Float>> rating1, HashMap<Integer, HashMap<Integer, Integer>> rating2, int numberOfRatings)
	{
		HashMap<Integer, Float> totalError= new HashMap<Integer, Float>();
		HashMap<Integer, Integer> movieIdToNumberRated=new HashMap<Integer, Integer>();
		
		for(int userId: rating1.keySet())
		{
			for(int movieId: rating1.get(userId).keySet())
			{
				if(rating1.get(userId).keySet().size()<numberOfRatings)
				{
					continue;
				}
				
				if(totalError.get(movieId)==null)
				{
					totalError.put(movieId, 0f);
					movieIdToNumberRated.put(movieId, 0);
				}
				totalError.put(movieId, totalError.get(movieId)+
				Math.abs(rating1.get(userId).get(movieId)-rating2.get(userId).get(movieId)));
				
				movieIdToNumberRated.put(movieId, movieIdToNumberRated.get(movieId)+1);
			}
		}
		
		HashMap<Integer, Float> averageError=new HashMap<Integer, Float>();
		for(int movieId: totalError.keySet())
		{
			averageError.put(movieId, (float) totalError.get(movieId)/ (float) movieIdToNumberRated.get(movieId));
		}
		
		return averageError;
	}
	/*
	 * Calculates The worst and best 5 movies that our program recommended
	 */
	public void printBottomAndTop10(int k, boolean user, int threshold, int numberOfRatings)
	{
		HashMap<Integer, HashMap<Integer, Float>> reccomendation;
		if(user)
			reccomendation=reccomendByUser(k, threshold);
		else
			reccomendation=reccomendByItem(k, threshold);
		fixAverage(reccomendation, true);
		HashMap<Integer, Float> accuracy=accuracy(reccomendation, testData, numberOfRatings);
		
		ArrayList<Integer> allIds=new ArrayList<Integer>(accuracy.keySet());
		allIds.sort(new Comparator<Integer>() {
			@Override
			public int compare(Integer o1, Integer o2) {
				if(accuracy.get(o2)-accuracy.get(o1)>0)
					return (int) Math.ceil(accuracy.get(o2)-accuracy.get(o1));
				else
					return (int) Math.floor(accuracy.get(o2)-accuracy.get(o1));
			}
		});
		for(int id: allIds)
		{
			if(accuracy.get(id)<0)
			{
				System.out.println("Bad id: "+id);
			}
		}
		
		
		fixAverage(movieToUserToRating, false);
		
		for(int i=1; i<=10; i++)
		{
			System.out.println("Most inaccurate "+i+": "+movieIdToName.get(allIds.get(i))+" Accuracy Difference: "+accuracy.get(allIds.get(i)));
			System.out.println(frequencyToCount(allIds.get(i)));
		}
		
		for(int i=1; i<=10; i++)
		{
			System.out.println("Most accurate "+i+": "+movieIdToName.get(allIds.get(allIds.size()-i))+" Accuracy Difference: "+accuracy.get(allIds.get(allIds.size()-i)));
			System.out.println(frequencyToCount(allIds.get(allIds.size()-i)));
		}
		
		System.out.println("RMS Error: "+RootMeanSquareError(reccomendation));
	}
	/*
	 * Returns the frequency of each rating 1-5 for a movie
	 */
	
	private HashMap<Integer, Integer> frequencyToCount(int movieId)
	{
		HashMap<Integer, Integer> frequencyToCount=new HashMap<Integer, Integer>();
		
		for(Float rating: movieToUserToRating.get(movieId).values())
		{
			int rounded=(int) Math.round(rating);
			if(frequencyToCount.get(rounded)==null)
				frequencyToCount.put(rounded, 0);
			frequencyToCount.put(rounded, frequencyToCount.get(rounded)+1);
		}
		return frequencyToCount;
	}
	
	public void printFrequencyMaps(ArrayList<Integer> ids)
	{
		for(int id: ids)
		{
			System.out.println(id+"\n"+frequencyToCount(id));
		}
	}
	
	/*
	 * Method to calculate the RMSE given our recommendation and the actual ratings
	 */
	
	public float RootMeanSquareError(HashMap<Integer, HashMap<Integer, Float>> reccomendation)
	{
		float totalError=0;
		float count=0f;
		for(int userId: reccomendation.keySet())
		{
			for(int movieId: reccomendation.get(userId).keySet())
			{
				totalError += Math.pow(reccomendation.get(userId).get(movieId) - testData.get(userId).get(movieId),2);
				count++;
			}
		}
		return (float) Math.sqrt(totalError/count);
	}
	
	public static void main(String[] args)
	{
		Recommender recommend=new Recommender();
		Scanner scan=new Scanner(System.in);
		System.out.print("User-based or item-based recommendation (u/i)>");
		boolean user=(scan.next().equalsIgnoreCase("u"));
		System.out.print("Enter k>");
		int k=scan.nextInt();
		System.out.print("Enter number of things in common to be nearest neighbors>");
		int threshold=scan.nextInt();
		System.out.print("Enter number of ratings threshold>");
		int numberOfRatings=scan.nextInt();
		recommend.printBottomAndTop10(k, user, threshold, numberOfRatings);
	}
}
