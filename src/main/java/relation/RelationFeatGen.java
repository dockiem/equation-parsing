package relation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.math.NumberUtils;

import structure.Equation;
import structure.EquationSolver;
import structure.Operation;
import utils.FeatGen;
import utils.Tools;
import edu.illinois.cs.cogcomp.core.datastructures.IntPair;
import edu.illinois.cs.cogcomp.core.datastructures.Pair;
import edu.illinois.cs.cogcomp.edison.sentences.Constituent;
import edu.illinois.cs.cogcomp.edison.sentences.Sentence;
import edu.illinois.cs.cogcomp.edison.sentences.TextAnnotation;
import edu.illinois.cs.cogcomp.quant.driver.QuantSpan;
import edu.illinois.cs.cogcomp.sl.core.AbstractFeatureGenerator;
import edu.illinois.cs.cogcomp.sl.core.IInstance;
import edu.illinois.cs.cogcomp.sl.core.IStructure;
import edu.illinois.cs.cogcomp.sl.util.IFeatureVector;
import edu.illinois.cs.cogcomp.sl.util.Lexiconer;

public class RelationFeatGen extends AbstractFeatureGenerator implements
		Serializable {
	private static final long serialVersionUID = 1810851154558168679L;
	public Lexiconer lm = null;

	public RelationFeatGen(Lexiconer lm) {
		this.lm = lm;
	}
	
	@Override
	public IFeatureVector getFeatureVector(IInstance arg0, IStructure arg1) {
		RelationX blob = (RelationX) arg0;
		RelationY relationY= (RelationY) arg1;
		List<String> features = new ArrayList<>();
		for(int i=0; i<relationY.relations.size(); ++i) {
			features.addAll(getFeatures(blob, relationY, i));
		}
		return FeatGen.getFeatureVectorFromList(features, lm);
	}

	// Cluster Features
	public IFeatureVector getFeatureVector(
			RelationX blob, RelationY labelSet, int index) {
		List<String> feats = getFeatures(blob, labelSet, index);
		return FeatGen.getFeatureVectorFromList(feats, lm);
	}
	
	public List<String> getFeatures(
			RelationX blob, RelationY labelSet, int index) {
		List<String> features = new ArrayList<>();
		String prefix = labelSet.relations.get(index);
		QuantSpan qs = blob.quantities.get(index);
		int pos = blob.ta.getTokenIdFromCharacterOffset(qs.start);
		for(String feature : FeatGen.neighboringTokens(blob.lemmas, pos, 3)) {
			features.add(prefix + "_" + feature);
		}
		return features;
	}
}