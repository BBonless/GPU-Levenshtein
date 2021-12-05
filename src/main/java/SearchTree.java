public class SearchTree {

    public SearchTree Less;
    public LevenshteinData Data;
    public SearchTree More;

    public SearchTree(LevenshteinData DataIn) {
        Data = DataIn;
    }

    /*public void Insert(LevenshteinData DataIn) {
        BST Pointer = this;
        BST Trail = null;
        BST NewNode = new BST(DataIn);

        while (Pointer != null) {
            Trail = Pointer;

            if (NewNode.Data.Distance > Pointer.Data.Distance) {
                Pointer = Pointer.More;
            }
            else if (NewNode.Data.Distance == Pointer.Data.Distance) {
                if (NewNode.Data.Ratio > Pointer.Data.Distance) {
                    Pointer = Pointer.More;
                }
                else {
                    Pointer = Pointer.Less;
                }
            }
            else
            {
                Pointer = Pointer.Less;
            }
        }

        if (NewNode.Data.Distance > Trail.Data.Distance) {
            Trail.More = NewNode;
        }
        else if (NewNode.Data.Distance == Trail.Data.Distance) {
            if (NewNode.Data.Ratio > Trail.Data.Ratio) {
                Trail.More = NewNode;
            }
            else {
                Trail.Less = NewNode;
            }
        }
        else
        {
            Trail.Less = NewNode;
        }
    }*/

    /*public void Inorder() {
        Stack<BST> Stack = new Stack<>();
        BST Current = this;
        int Count = 0;

        while (Current != null || Stack.size() > 0) {
            while (Current != null) {
                Stack.push(Current);
                Current = Current.Less;
            }

            Current = Stack.pop();

            Current.Data.Out();
            Count++;

            Current = Current.More;
        }

        System.out.println("Count = " + Count);

    }*/

    public void Insert(SearchTree Node) {
        String Direction;

        if (Node.Data.Score > Data.Score) {
            Direction = "More";
        }
        else {
            Direction = "Less";
        }

        if (Direction.equals("More")) {
            if (More == null) {
                More = Node;
            }
            else {
                More.Insert(Node);
            }
        }
        else if (Direction.equals("Less")) {
            if (Less == null) {
                Less = Node;
            }
            else {
                Less.Insert(Node);
            }
        }
        else {
            throw new IllegalArgumentException();
        }
    }

    public void PrintInorder(SearchTree Node, int[] Limit) {
        if (Node == null) {
            return;
        }
        else {
            if (Limit[0] >= Limit[1]) {
                return;
            }
            PrintInorder(Node.Less, Limit);
            if (Limit[0] < Limit[1]) {
                if (!Node.Data.Word.equals("#")) {
                    Node.Data.Out();
                }
                else {
                    Limit[0]--;
                }
            }
            Limit[0]++;
            PrintInorder(Node.More, Limit);
        }
    }

}
