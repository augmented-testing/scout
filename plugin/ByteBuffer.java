package plugin;

import java.util.Vector;

public class ByteBuffer
{
	private final static int BYTES_IN_BLOCK=500; 
	private Vector<byte[]> blocks;
	private byte[] lastBlock;
	private int noBytesTotal=0;
	private int noBytesLastBlock=0;

	ByteBuffer()
	{
		blocks=new Vector<byte[]>();
		lastBlock=new byte[BYTES_IN_BLOCK];
		blocks.add(lastBlock);
	}

	public void addByte(byte b)
	{
		if(noBytesLastBlock==BYTES_IN_BLOCK)
		{
			// Last block is full
			noBytesLastBlock=0;
			// Create new block
			lastBlock=new byte[BYTES_IN_BLOCK];
			blocks.add(lastBlock);
		}
		// Add byte
		lastBlock[noBytesLastBlock]=b;
		// Increase counters
		noBytesTotal++;
		noBytesLastBlock++;
	}

	public byte[] getByteArray()
	{
		byte[] array=new byte[noBytesTotal];
		int noAdded=0;

		for(int blockNo=0; blockNo<blocks.size(); blockNo++)
		{
			byte[] block=(byte[])blocks.elementAt(blockNo);
			for(int i=0; i<block.length; i++)
			{
				array[noAdded]=block[i];
				noAdded++;
				if(noAdded==noBytesTotal)
				{
					// Ready
					return array;
				}
			}
		}
		// Should not happen
		return array;
	}
}
