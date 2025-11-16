package ca.concordia.filesystem.datastructures;

public class FNode {

    private short blockIndex;
    private int next;

    public FNode(short blockIndex) {
        this.blockIndex = blockIndex;
        this.next = -1;
    }

    public int getBlockIndex(){
        return blockIndex;
    }

    public void setBlockIndex(short blockIndex){
        this.blockIndex = blockIndex;
    }

        public int getNext(){
        return next;
    }

    public void setNext(short next){
        this.next = next;
    }
    

}
