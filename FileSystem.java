import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;
import java.lang.IllegalArgumentException;

public class FileSystem{

	private static final int FAT_SIZE = 64;
	private int[] FAT = new int[FAT_SIZE];
	private List<Inode> inodes = new ArrayList<>();
	private long bitmap = 0;

	public static void main(String[] args){
		new FileSystem();
	}

	public FileSystem(){
		String command = "command";
		Scanner kb = new Scanner(System.in);
		while(!command.equalsIgnoreCase("exit")){
			System.out.print("> ");
			command = kb.nextLine();
			String[] commandArgs = command.split(" ");
			switch(commandArgs[0].toLowerCase()){
				case "put":
					String[] putArgs = commandArgs[1].split(",");
					try{
						put(putArgs[0], Integer.parseInt(putArgs[1]));
					}
					catch(IllegalArgumentException e){
						System.out.println(e.getMessage());
					}
					break;
				case "del":
					try{
						delete(commandArgs[1]);
					}
					catch(IllegalArgumentException e){
						System.out.println(e.getMessage());
					}
					break;
				case "bitmap":
					printBitmap();
					break;
				case "inodes":
					printInodes();
					break;
				case "exit":
					System.out.println("Goodbye!");
					break;
				default:
					System.out.println("Illegal input!");
					break;
			}
		}
	}

	private void put(String filename, int fileSize) throws IllegalArgumentException{
		for(int i = 0; i < inodes.size(); i++){
			if(filename.equals(inodes.get(i).name)){
				throw new IllegalArgumentException("File already exists.");
			}
		}
		List<Integer> freeBlocks = new ArrayList<>();
		for(int i = 0; freeBlocks.size() < fileSize && i < FAT_SIZE; i++){
			if(isFree(i)){
				freeBlocks.add(i);
			}
		}
		if(freeBlocks.size() == fileSize){
			allocate(filename, freeBlocks, fileSize);
		}
		else{
			throw new IllegalArgumentException(fileSize  + " block(s) not found.  "
												+ freeBlocks.size() + "/64 blocks free.");
		}
	}

	private boolean isFree(int block){
		return ((bitmap >> block) & 1) == 0;
	}

	private void allocate(String filename, List<Integer> freeBlocks, int fileSize){
		inodes.add(new Inode(filename, freeBlocks.get(0), fileSize));
		for(int i = 0; i < fileSize; i++){
			setAllocated(freeBlocks.get(i));
			if(i == fileSize - 1){
				FAT[freeBlocks.get(i)] = -1;
			}
			else{
				FAT[freeBlocks.get(i)] = freeBlocks.get(i + 1);
			}
		}
	}

	private void setAllocated(int block){
		bitmap |= 1L << block;
	}

	private void delete(String filename) throws IllegalArgumentException{
		int index = inodes.indexOf(new Inode(filename, 0, 0));
		if(index == -1){
			throw new IllegalArgumentException("File not found.");
		}
		else{
			Inode deleted = inodes.remove(index);
			int nextLink = deleted.fileStart;
			do{
				setFree(nextLink);
				nextLink = FAT[nextLink];
			}while(nextLink != -1);
		}
	}

	private void setFree(int block){
		bitmap &= ~(1L << block);
	}

	private void printBitmap(){
		String binBitmap = String.format("%64s", Long.toBinaryString(bitmap))
								 .replace(' ', '0');
		for(int i = 0; i < FAT_SIZE; i += 8){
			System.out.printf("%2d ", i);
			for(int j = i; j < i + 8; j++){
				System.out.print(binBitmap.charAt(FAT_SIZE - 1 - j));
			}			
			System.out.println();
		}
	}

	private void printInodes(){
		for(Inode i : inodes){
			int nextLink = i.fileStart;
			System.out.print(i.name + ": ");
			do{
				System.out.print(nextLink);
				nextLink = FAT[nextLink];
				if(nextLink != -1){
					System.out.print(" -> ");
				}
			}while(nextLink != -1);
			System.out.println();
		}
		if(inodes.isEmpty()){
			System.out.println("No files in system.");
		}
	}

	class Inode{
		
		protected String name;
		protected int fileStart;
		protected int totalBlocks;

		public Inode(String name, int fileStart, int totalBlocks){
			this.name = name;
			this.fileStart = fileStart;
			this.totalBlocks = totalBlocks;
		}

		@Override
		public boolean equals(Object that){
			if(that == this){
				return true;
			}
			if(!(that instanceof Inode)){
				return false;
			}
			Inode thatInode = (Inode)that;
			return this.name.equals(thatInode.name);
		}
	}

}
