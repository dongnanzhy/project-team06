package Annotator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSList;
import org.apache.uima.jcas.cas.TOP;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;

import util.Utils;

import com.google.common.util.concurrent.Service;

import edu.cmu.lti.oaqa.bio.bioasq.services.GoPubMedService;
import edu.cmu.lti.oaqa.bio.bioasq.services.OntologyServiceResponse;
import edu.cmu.lti.oaqa.bio.bioasq.services.OntologyServiceResponse.Concept;
import edu.cmu.lti.oaqa.bio.bioasq.services.OntologyServiceResponse.Finding;
import edu.cmu.lti.oaqa.type.input.Question;
import edu.cmu.lti.oaqa.type.retrieval.AtomicQueryConcept;
import edu.cmu.lti.oaqa.type.retrieval.ComplexQueryConcept;
import edu.cmu.lti.oaqa.type.retrieval.ConceptSearchResult;
import edu.cmu.lti.oaqa.type.retrieval.QueryOperator;

/**
 * The SDQuestionConceptAnnotator uses OntologyServiceResponse service to
 * process the query content and return several features which corresponds to
 * the data type provided by project archetype. Finally, store these information
 * into ConceptSearchResult-typed annotation.
 *
 */
public class SDQuestionConceptAnnotator extends JCasAnnotator_ImplBase {
	/** The GoPubMedService instance variable */
	public GoPubMedService service;
	
	/**
	 * The initialize method initialize the GoPubMedService instance using a preset profile.
	 * 
	 * @see org.apache.uima.analysis_component.JCasAnnotator_ImplBase#initialize(UimaContext)
	 */
	public void initialize(UimaContext aContext)
			throws ResourceInitializationException {
		super.initialize(aContext);
		try {
			service = new GoPubMedService("project.properties");
		} catch (Exception ex) {
			throw new ResourceInitializationException();
		}
	}

	@Override
	/**
	 * The process performs the main task that stores the information provided by OntologyServiceResponse 
	 * into CAS.
	 * 
	 * @see org.apache.uima.analysis_component.JCasAnnotator_ImplBase#process(JCas)
	 */
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
		FSIterator<TOP> it = aJCas.getJFSIndexRepository().getAllIndexedFS(
				ComplexQueryConcept.type);
		while (it.hasNext()) {
			ComplexQueryConcept con = (ComplexQueryConcept) it.next();
			FSList conceptFslist = con.getOperatorArgs();
			ArrayList<AtomicQueryConcept> conceptArray = Utils.fromFSListToCollection(conceptFslist, AtomicQueryConcept.class);
			QueryOperator operator = con.getOperator();
			String queryText = "";
			queryText = conceptArray.get(0).getText();
			if(conceptArray.size() != 1){
				int index = 1;
				while(index < conceptArray.size()){
					queryText += operator;
					queryText += conceptArray.get(index).getText();
					index++;
				}
			}
			
			try {
				OntologyServiceResponse.Result result = service
						.findMeshEntitiesPaged(queryText, 0);
				int curRank = 0;
				for (Finding finding : result.getFindings()) {
					edu.cmu.lti.oaqa.type.kb.Concept concept = new edu.cmu.lti.oaqa.type.kb.Concept(
							aJCas);
					concept.setName(finding.getConcept().getLabel());
					//System.out.println(finding.getConcept().getLabel());
					concept.addToIndexes();

					ConceptSearchResult result1 = new ConceptSearchResult(aJCas);
					result1.setConcept(concept);
					result1.setUri(finding.getConcept().getUri());
					result1.setScore(finding.getScore());
					result1.setText(finding.getConcept().getLabel());
					result1.setRank(curRank++);
					result1.setQueryString(queryText);
					result1.addToIndexes();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	
/*	List<Finding> combine(List<Finding> f1, List<Finding> f2) {
	    
		List<String> queryList = new ArrayList<String>();

	    for (String s : query.split("\\s+"))
	      queryList.add(s);
	    return queryList;
}*/

}
