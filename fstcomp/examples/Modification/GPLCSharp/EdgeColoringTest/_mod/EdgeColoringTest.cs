class EdgeColoringTest {
    public static void Main(string[] args) {	
	Graph edgeColorTestGraph = new Graph();
	Vertex v1 = new Vertex();
	v1.name = "v1";
	Vertex v2 = new Vertex();
	v2.name = "v2";
	Vertex v3 = new Vertex();
	v3.name = "v3";
	Vertex v4 = new Vertex();
	v4.name = "v4";
	Vertex v5 = new Vertex();
	v5.name = "v5";

	edgeColorTestGraph.AddVertex(v1);
	edgeColorTestGraph.AddVertex(v2);
	edgeColorTestGraph.AddVertex(v3);
	edgeColorTestGraph.AddVertex(v4);
	edgeColorTestGraph.AddVertex(v5);

	edgeColorTestGraph.AddEdge(v1, v2);
	edgeColorTestGraph.AddEdge(v1, v3);
	edgeColorTestGraph.AddEdge(v1, v4);
	edgeColorTestGraph.AddEdge(v2, v5);
	edgeColorTestGraph.AddEdge(v3, v4);
	edgeColorTestGraph.AddEdge(v5, v4);

	edgeColorTestGraph.display();
	edgeColorTestGraph.edgeColoring();	
	edgeColorTestGraph.display();	
    }

}
