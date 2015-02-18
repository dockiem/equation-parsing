package semparse;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import structure.Equation;
import structure.EquationSolver;
import structure.Node;
import structure.SimulProb;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.sl.core.IStructure;

public class SemY implements IStructure, Serializable {
	
	private static final long serialVersionUID = 2399969922362221136L;
	public List<Equation> equations;
	public List<Node> nodes;
	
	public SemY() {
		equations = new ArrayList<>();
		nodes = new ArrayList<>();
	}
	
	public SemY(SemY other) {
		equations = new ArrayList<>();
		for(Equation eq : other.equations) {
			equations.add(new Equation(eq));
		}
		nodes = new ArrayList<>();
		nodes.addAll(other.nodes);
	}
	
	public SemY(SimulProb prob) {
		equations = new ArrayList<>();
		for(Equation eq : prob.equations) {
			equations.add(eq);
		}
		nodes = new ArrayList<Node>();
		nodes.addAll(prob.nodes);
	}
	
	public static float getNodeLoss(SemY y1, SemY y2) {
		float loss = 0.0f;
		for(Node pair1 : y1.nodes) {
			boolean found = false;
			for(Node pair2 : y2.nodes) {
				if(pair1.label.equals(pair2.label) && 
						pair1.span.equals(pair2.span)) {
					found = true;
					break;
				}
			}
			if(!found) loss += 1.0;
		}
		for(Node pair1 : y2.nodes) {
			boolean found = false;
			for(Node pair2 : y1.nodes) {
				if(pair1.label.equals(pair2.label) && 
						pair1.span.equals(pair2.span)) {
					found = true;
					break;
				}
			}
			if(!found) loss += 1.0;
		}
		return loss;
	}
	
	public static float getEquationLoss(SemY y1, SemY y2) {
		if(y1.equations.size() != y2.equations.size()) return 10.0f;
		if(y1.equations.size() == 1) return Equation.getLoss(
				y1.equations.get(0), y2.equations.get(0));
		if(y1.equations.size() == 2) {
			float loss1 = Equation.getLoss(y1.equations.get(0), y2.equations.get(0)) + 
					Equation.getLoss(y1.equations.get(1), y2.equations.get(1));
			float loss2 = Equation.getLoss(y1.equations.get(0), y2.equations.get(1)) + 
					Equation.getLoss(y1.equations.get(1), y2.equations.get(0));
			return Math.min(loss1, loss2);
		}
		return 10.0f;		
	}
	
	public static float getSolutionLoss(SemY y1, SemY y2) {
		if(EquationSolver.doesHaveSameSolution(y1.equations, y2.equations)) {
			return 0.0f;
		}
		return 5.0f;
	}
	
	public static float getLoss(SemY y1, SemY y2) {
		return getNodeLoss(y1, y2);
	}
	
	@Override
	public String toString() {
		return ""+Arrays.asList(nodes);
	}
}