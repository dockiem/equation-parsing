package template;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import reader.DocReader;
import structure.Equation;
import structure.Node;
import structure.SimulProb;
import tree.TreeDriver;
import tree.TreeInfSolver;
import tree.TreeX;
import tree.TreeY;
import utils.Params;
import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.core.SLModel;
import edu.illinois.cs.cogcomp.sl.core.SLParameters;
import edu.illinois.cs.cogcomp.sl.core.SLProblem;
import edu.illinois.cs.cogcomp.sl.learner.Learner;
import edu.illinois.cs.cogcomp.sl.learner.LearnerFactory;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class TemplateDriver {
	
	public static void crossVal() throws Exception {
		double acc = 0.0;
		for(int i=0;i<5;i++) {
			acc += doTrainTest(i);
		}
		System.out.println("5-fold CV : " + (acc/5));
	}
	
	public static double doTrainTest(int testFold) throws Exception {
		List<List<Integer>> folds = DocReader.extractFolds();
		List<SimulProb> simulProbList = 
				DocReader.readSimulProbFromBratDir(Params.annotationDir);
		List<SimulProb> trainProbs = new ArrayList<>();
		List<SimulProb> testProbs = new ArrayList<>();
		for(SimulProb simulProb : simulProbList) {
			if(folds.get(testFold).contains(simulProb.index)) {
				testProbs.add(simulProb);
			} else {
				trainProbs.add(simulProb);
			}
		}
		SLProblem train = getSP(trainProbs);
		SLProblem test = getSP(testProbs);
		trainModel("models/joint"+testFold+".save", train, testFold);
		return testModel("models/joint"+testFold+".save", test);
	}
	
	public static SLProblem getSP(List<SimulProb> simulProbList) 
			throws Exception {
		if(simulProbList == null) {
			simulProbList = 
					DocReader.readSimulProbFromBratDir(Params.annotationDir);
		}
		SLProblem problem = new SLProblem();
		for (SimulProb simulProb : simulProbList) {
			List<IntPair> eqSpans = TreeDriver.extractGoldEqSpans(simulProb);
			List<String> eqStrings = TemplateDriver.extractGoldEqStrings(simulProb, eqSpans);
			TemplateX x = new TemplateX(simulProb, eqStrings);
			TemplateY y = new TemplateY(simulProb);
			problem.addExample(x, y);
		}
		return problem;
	}

	public static double testModel(String modelPath, SLProblem sp)
			throws Exception {
		SLModel model = SLModel.loadModel(modelPath);
		Set<Integer> incorrect = new HashSet<>();
		Set<Integer> total = new HashSet<>();
		double acc = 0.0;
		for (int i = 0; i < sp.instanceList.size(); i++) {
			TemplateX prob = (TemplateX) sp.instanceList.get(i);
			TemplateY gold = (TemplateY) sp.goldStructureList.get(i);
			TemplateY pred = (TemplateY) model.infSolver.getBestStructure(
					model.wv, prob);
			total.add(prob.problemIndex);
			double goldWt = model.wv.dotProduct(
					model.featureGenerator.getFeatureVector(prob, gold));
			double predWt = model.wv.dotProduct(
					model.featureGenerator.getFeatureVector(prob, pred));
			if(goldWt > predWt) {
				System.out.println("PROBLEM HERE");
			}
			if(TemplateY.getLoss(gold, pred) < 0.0001) {
				acc += 1;
			} else {
				incorrect.add(prob.problemIndex);
				System.out.println(prob.problemIndex+" : "+prob.ta.getText());
				System.out.println("Skeleton : "+Tools.skeletonString(prob.skeleton));
				System.out.println("Quantities : "+prob.quantities);
				System.out.println("Gold : \n"+gold);
				System.out.println("Gold weight : "+model.wv.dotProduct(
						model.featureGenerator.getFeatureVector(prob, gold)));
				System.out.println("Pred : \n"+pred);
				System.out.println("Pred weight : "+model.wv.dotProduct(
						model.featureGenerator.getFeatureVector(prob, pred)));
				System.out.println("Loss : "+TemplateY.getLoss(gold, pred));
			}
		}
		System.out.println("Accuracy : = " + acc + " / " + sp.instanceList.size() 
				+ " = " + (acc/sp.instanceList.size()));
		return (acc/sp.instanceList.size());
	}
	
	public static void trainModel(String modelPath, SLProblem train, int testFold) 
			throws Exception {
		SLModel model = new SLModel();
		Lexiconer lm = new Lexiconer();
		lm.setAllowNewFeatures(true);
		model.lm = lm;
		TemplateFeatGen fg = new TemplateFeatGen(lm);
		model.featureGenerator = fg;
		model.infSolver = new TemplateInfSolver(fg, extractTemplates(train));
		SLParameters para = new SLParameters();
		para.loadConfigFile(Params.spConfigFile);
		Learner learner = LearnerFactory.getLearner(model.infSolver, fg, para);
		model.wv = learner.train(train);
		lm.setAllowNewFeatures(false);
		model.saveModel(modelPath);
	}
	
	public static void main(String args[]) throws Exception {
		TemplateDriver.doTrainTest(0);
//		JointDriver.crossVal();
	}
	
	public static List<List<Equation>> extractTemplates(SLProblem slProb) {
		List<List<Equation>> templates = new ArrayList<>();
		for(IStructure struct : slProb.goldStructureList) {
			TemplateY gold = new TemplateY((TemplateY) struct);
			for(Equation eq1 : gold.equations) {
				for(int j=0; j<5; ++j) {
					for(int k=0; k<eq1.terms.get(j).size(); ++k) {
						eq1.terms.get(j).get(k).setSecond(null);
					}
				}
			}
			boolean alreadyPresent = false;
			for(int i=0; i< templates.size(); ++i) { 
				TemplateY y = new TemplateY();
				y.equations = templates.get(i); 
				if(TemplateY.getEquationLoss(gold, y) < 0.0001) {
					alreadyPresent = true;
					break;
				}
			}
			if(!alreadyPresent) {
				templates.add(gold.equations);
			}
		}
		System.out.println("Number of templates : "+templates.size());
		return templates;
	}
	
	public static List<Template> extractGraftedTemplates(
			TemplateX x, List<List<Equation>> templates, List<String> eqStrings) {
		List<Template> relevantTemplates = new ArrayList<>();
		List<Equation> mathEquations = new ArrayList<>();
		for(String eqString : eqStrings) {
			mathEquations.add(new Equation(0, eqString));
 		}
		for(List<Equation> template : templates) {
			List<Equation> graft = extractedGraftedTemplate(
					template, mathEquations);
			System.out.println("Template : "+template);
			System.out.println("Math Equations : "+mathEquations);
			System.out.println("Graft : "+graft);
			if(graft != null) {
				relevantTemplates.add(new Template(graft));
			}
		}
		return relevantTemplates;
	}
	
	// Greedy matching should work
	public static List<Equation> extractedGraftedTemplate(
			List<Equation> template, List<Equation> mathEquations) {
		List<Equation> graft = new ArrayList<>();
		for(Equation eq : template) {
			graft.add(new Equation(eq));
		}
		List<IntPair> match = new ArrayList<>();
		boolean allFound = true;
		for(Equation eq : mathEquations) {
			boolean found = false;
			Equation eq1 = new Equation(eq);
			for(int j=0; j<5; ++j) {
				for(int k=0; k<eq1.terms.get(j).size(); ++k) {
					eq1.terms.get(j).get(k).setSecond(null);
				}
			}
			for(int i=0; i<template.size(); ++i) {
				if(Equation.getLoss(template.get(i), eq1) < 0.01 && 
						!match.contains(new IntPair(i, 2))) {
					graft.set(i, eq);
					match.add(new IntPair(i, 2));
					found = true;
					break;
				}
			}
			if(found) continue;
			for(int i=0; i<template.size(); ++i) {
				if(partialEquationMatch(template.get(i), eq1, 0) && 
						!match.contains(new IntPair(i, 0))) {
					graft.get(i).terms.set(0, eq1.terms.get(0));
					graft.get(i).terms.set(1, eq1.terms.get(1));
					graft.get(i).operations.set(0, eq1.operations.get(0));
					graft.get(i).operations.set(1, eq1.operations.get(1));
					match.add(new IntPair(i, 0));
					found = true;
					break;
				}
				if(partialEquationMatch(template.get(i), eq1, 1) && 
						!match.contains(new IntPair(i, 1))) {
					graft.get(i).terms.set(2, eq1.terms.get(0));
					graft.get(i).terms.set(3, eq1.terms.get(1));
					graft.get(i).operations.set(2, eq1.operations.get(0));
					graft.get(i).operations.set(3, eq1.operations.get(1));
					match.add(new IntPair(i, 1));
					found = true;
					break;
				}
			}
			if(!found) {
				allFound = false;
				break;
			}
		}
		if(allFound) {
			return graft;
		}
		return null;
	}
	
	public static boolean partialEquationMatch(
			Equation template, Equation eq, int index) {
		if(template.terms.get(4).size() > 0) return false;
		if(Equation.getLossPairLists(template.terms.get(2*index), eq.terms.get(0)) < 0.01) {
			if(Equation.getLossPairLists(template.terms.get(2*index+1), eq.terms.get(1)) < 0.01) {
				if(template.operations.get(2*index) == eq.operations.get(0) &&
						template.operations.get(2*index+1) == eq.operations.get(1)) {
					return true;		
				}
			}
		}
		return false;
	}
	
	public static List<String> extractGoldEqStrings(
			SimulProb simulProb, List<IntPair> eqSpans) {
		List<String> eqStrings = new ArrayList<>();
		for(IntPair span : eqSpans) {
			TreeX x = new TreeX(simulProb, span);
			TreeY y = new TreeY(simulProb, span);
			Node maxNode = null;
			int maxSize = 0;
			for(Node node : y.nodes) {
				if(node.span.getSecond() - node.span.getFirst() > maxSize) {
					maxSize = node.span.getSecond() - node.span.getFirst();
					maxNode = node;
				}
			}
			eqStrings.add(TreeInfSolver.postProcessEqString(TreeInfSolver.getEqString(x, maxNode)));
//			System.out.println("EqString : "+eqStrings.get(eqStrings.size()-1));
//			System.out.println("Problem Index : "+simulProb.index);
//			System.out.println("Text : "+simulProb.ta.getText());
//			System.out.println("Triggers : "+simulProb.triggers);
//			System.out.println("Node : "+y.nodes);
		}
		return eqStrings;
	}
}