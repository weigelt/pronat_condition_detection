package edu.kit.ipd.parse.conditionDetection;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import edu.kit.ipd.parse.graphBuilder.GraphBuilder;
import edu.kit.ipd.parse.luna.data.MissingDataException;
import edu.kit.ipd.parse.luna.data.PrePipelineData;
import edu.kit.ipd.parse.luna.data.token.MainHypothesisToken;
import edu.kit.ipd.parse.luna.data.token.Token;
import edu.kit.ipd.parse.luna.graph.IGraph;
import edu.kit.ipd.parse.luna.graph.INode;
import edu.kit.ipd.parse.luna.pipeline.PipelineStageException;
import edu.kit.ipd.parse.multiasr.asr.ASROutput;
import edu.kit.ipd.parse.multiasr.asr.GoogleASR;
import edu.kit.ipd.parse.multiasr.asr.WatsonASR;
import edu.kit.ipd.parse.multiasr.asr.spi.IASR;
import edu.kit.ipd.parse.shallownlp.ShallowNLP;

public class GoogleASRTest {

	private static final String TEST_FILE_PATH = "/home/seb/seb/vcs/GIT/parse/Abschlussarbeiten/steurer_ba/Evaluation/audios/scene5_19.flac";
	ConditionDetector dt;
	Token[] actual;
	ShallowNLP snlp;
	String input;
	IGraph graph;
	PrePipelineData ppd;
	GraphBuilder gb;

	@Before
	public void setUp() {
		snlp = new ShallowNLP();
		snlp.init();
		dt = new ConditionDetector();
		dt.init();
		ppd = new PrePipelineData();
		gb = new GraphBuilder();
		gb.init();
	}

	@Ignore
	@Test
	public void evalWithGoogleASR() {

		final GoogleASR asr = new GoogleASR();
		final HashMap<String, String> capabilities = new HashMap<>();
		capabilities.put("NBEST", "5");
		List<ASROutput> results = null;
		try {
			results = test(asr, capabilities);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (ASROutput asrOutput : results) {
			for (MainHypothesisToken mainHypothesisToken : asrOutput) {
				System.out.println(mainHypothesisToken.getWord());
			}
		}

		if (results != null && results.size() > 0) {
			ppd.setMainHypothesis(results.get(0));
		}
		try {
			snlp.exec(ppd);
		} catch (PipelineStageException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			gb.exec(ppd);
		} catch (PipelineStageException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			dt.setGraph(ppd.getGraph());
		} catch (MissingDataException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		dt.exec();

		List<INode> actual = dt.getGraph().getNodes();
		for (INode iNode : actual) {
			System.out.println(iNode.toString());
		}
	}

	@Ignore
	@Test
	public void evalWithWatsonASR() {

		final WatsonASR asr = new WatsonASR();
		final HashMap<String, String> capabilities = new HashMap<>();
		capabilities.put("NBEST", "5");
		List<ASROutput> results = null;
		try {
			results = test(asr, capabilities);
		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		for (ASROutput asrOutput : results) {
			for (MainHypothesisToken mainHypothesisToken : asrOutput) {
				System.out.println(mainHypothesisToken.getWord());
			}
		}
		List<MainHypothesisToken> wHypo = new ArrayList<>();
		if (results != null) {
			for (ASROutput asrOutput : results) {
				wHypo.addAll(asrOutput);
			}
		}
		ppd.setMainHypothesis(wHypo);
		try {
			snlp.exec(ppd);
		} catch (PipelineStageException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			gb.exec(ppd);
		} catch (PipelineStageException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			dt.setGraph(ppd.getGraph());
		} catch (MissingDataException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		dt.exec();

		List<INode> actual = dt.getGraph().getNodes();
		for (INode iNode : actual) {
			System.out.println(iNode.toString());
		}
	}

	public List<ASROutput> test(IASR asr, Map<String, String> capabilities) throws URISyntaxException, MalformedURLException {
		URL url = new File(TEST_FILE_PATH).toURI().toURL();

		List<ASROutput> results = asr.recognize(url.toURI(), Paths.get(url.toURI()), capabilities);

		return results;
	}
}
