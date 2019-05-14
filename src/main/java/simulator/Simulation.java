package simulator;

import java.io.File;
import java.util.BitSet;

import channel.CanaleSimmetricoBinario;
import channel.ChannelModel;
import channel.GilbertElliot;
import coder.channel.ChannelCoder;
import coder.channel.ChannelMessage;
import coder.source.SourceCoder;
import coders.LZW.funzionante.LZWCoder;
import coders.convolutional.ConvolutionalChannelCoder;
import coders.deflate.DeflateCoder;
import coders.hamming.HammingChannelCoder;
import coders.huffman.HuffmanCoder;
import coders.repetition.ConcatenatedChannelCoder;
import coders.repetition.RepChannelCoder;
import gui.ProvaGUI;
import utils.GenericUtils;
import utils.MyBitSet;
import utils.Statistics;

public class Simulation {

    private String fileInputPath;
    private ChannelModel channel;
    private ChannelCoder channelCoder;
    private SourceCoder sourceCoder;
    private Statistics statistics;

    public Simulation(SourceCoder sourceCoder, ChannelCoder channelCoder, ChannelModel channelModel,
            Statistics statistics, String fileInputPath) {

        this.sourceCoder = sourceCoder;
        this.channelCoder = channelCoder;
        this.channel = channelModel;
        this.statistics = statistics;

        this.fileInputPath = fileInputPath;

    }

    public static String outputPath(String inputPath) {
        return inputPath.substring(0, inputPath.length() - 4) + "_Decoded.txt";
    }

    public void execute() {
        statistics.setInitialTime(System.currentTimeMillis());

        //codifica
        String fileOutput = outputPath(fileInputPath);
        String sourceCode = fileInputPath.substring(0, fileInputPath.length() - 4) + "_TMP";

        sourceCoder.encode(fileInputPath, sourceCode);
        ProvaGUI.getInstance().getProgressBar().setValue(1);
        statistics.setSourceCodingTime(System.currentTimeMillis());
        ChannelMessage mess = GenericUtils.getChannelMessage(sourceCode);
        MyBitSet b = channelCoder.encode(mess);
        statistics.setChannelCodingTime(System.currentTimeMillis());
        ProvaGUI.getInstance().getProgressBar().setValue(2);
        
        //invio su canale
        MyBitSet corruptedBits = channel.send(b);
        ProvaGUI.getInstance().getProgressBar().setValue(3);
        
        //decodifica
        channelCoder.decode(corruptedBits, mess);
        statistics.setChannelDecodingTime(System.currentTimeMillis());
        GenericUtils.writeChannelMessage(mess, sourceCode + "2");
        ProvaGUI.getInstance().getProgressBar().setValue(4);
        sourceCoder.decode(sourceCode + "2", fileOutput);
        statistics.setSourceDecodingTime(System.currentTimeMillis());
        ProvaGUI.getInstance().getProgressBar().setValue(5);
        
        
        //dati utili alle statistiche (compressione e errorRate)
        byte[] sourceCodeBits = GenericUtils.getChannelMessage(sourceCode).getPayload();
        byte[] channelDecodedBits = GenericUtils.getChannelMessage(sourceCode + "2").getPayload();
        statistics.setSourceCoding(sourceCodeBits);
        statistics.setChannelDecoding(channelDecodedBits);
        //per le statistiche di robustezza del codificatore di canale - simuliamo un invio della codifica di sorgente senza quella di canale per vedere il tasso di errore
        BitSet onlySourceCoding = BitSet.valueOf(sourceCodeBits);
        MyBitSet onlySourceCodingMess = new MyBitSet(onlySourceCoding, sourceCodeBits.length*8);
        MyBitSet corruptedBitsOnlySourceCodingMess = channel.send(onlySourceCodingMess);
        statistics.setCorruptedSourceCodingWithoutChannelEncoding(corruptedBitsOnlySourceCodingMess.getBitset());

        statistics.setInitialSize(GenericUtils.getChannelMessage(fileInputPath).getPayloadLength());
        statistics.setSourceCodeSize(GenericUtils.getChannelMessage(sourceCode).getPayloadLength());
    }

    public String getFileInputPath() {
		return fileInputPath;
	}

	public ChannelModel getChannel() {
		return channel;
	}

	public ChannelCoder getChannelCoder() {
		return channelCoder;
	}

	public SourceCoder getSourceCoder() {
		return sourceCoder;
	}

	public Statistics getStatistics() {
		return statistics;
	}

	public static void main(String[] args) throws InstantiationException, IllegalAccessException {

        long t1 = System.currentTimeMillis();
        LZWCoder sourceCoder = new LZWCoder();
        DeflateCoder sourceCoder2 = new DeflateCoder();
        HuffmanCoder sourceCoder3 = new HuffmanCoder();
        HammingChannelCoder channelCoder = new HammingChannelCoder();

        ConvolutionalChannelCoder channelCoder2 = new ConvolutionalChannelCoder(7, 2);
        int []a = {3,3};
        ConcatenatedChannelCoder channelCoder3 = new ConcatenatedChannelCoder(a);
        RepChannelCoder channelCoder4 = new RepChannelCoder(5);
        
        CanaleSimmetricoBinario channel = new CanaleSimmetricoBinario(0.01);
        GilbertElliot channel2 = new GilbertElliot(GilbertElliot.SOFT);
        
        System.out.println(channelCoder4.getClass().getName());

        Simulation sim = new Simulation(sourceCoder3, channelCoder, channel, new Statistics(), "Lorem ipsum.txt");

        sim.execute();
        long t2 = System.currentTimeMillis();
        System.out.println("Sorgente: " + sim.sourceCoder.getClass() + " ,CodCanale: " + sim.channelCoder.getClass()
                + " ,modelloCanale: " + sim.channel.getClass());
        System.out.println("Ritardo: " + (t2 - t1) + " ms");
        System.out.println("ritardo cod sorg: " + sim.statistics.getSourceCodingTime() + " ms");
        System.out.println("ritardo cod canale: " + sim.statistics.getChannelCodingTime() + " ms");
        System.out.println("ritardo decod canale: " + sim.statistics.getChannelDecodingTime() + " ms");
        System.out.println("ritardo decod sorg: " + sim.statistics.getSourceDecodingTime() + " ms");

        System.out.println("\ndimensione file iniziale " + sim.statistics.getInitialSize() + " byte");
        System.out.println("dimensione file compressioneSorgente " + sim.statistics.getSourceCodeSize() + " byte");
        System.out.println("compression rate: " + sim.statistics.getCompressionRate() + "\n");

        System.out.println("error rate cod canale " + sim.statistics.getChannelDecodingErrorRate() * 100 + " %");
        System.out.println("error rate canale solo cod sorgente " + sim.statistics.getOnlySourceCodeChannelErrorRate() * 100 + " %");
        System.out.println("recovery rate del codificatore di canale " + sim.statistics.getErrorRecoveryRate() * 100 + " %");

    }

}
