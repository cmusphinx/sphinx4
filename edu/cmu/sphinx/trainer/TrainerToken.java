public class TrainerToken extends Token {
    private Collection linkToParent;
    private Collection linkToChild;
    private float alpha;
    private float beta;

    private UtteranceGraphNode state;
    private boolean isEmitting;
    private Feature dataVector;
    private int timeStamp;
}
