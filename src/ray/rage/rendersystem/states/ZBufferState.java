//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package ray.rage.rendersystem.states;

public interface ZBufferState extends RenderState {
    void setTestFunction(ZBufferState.TestFunction var1);

    ZBufferState.TestFunction getTestFunction();

    void setTestEnabled(boolean var1);

    boolean hasTestEnabled();

    void setWritable(boolean var1);

    boolean isWritable();

    int getSecondaryStage(); /* My Change: added a secondary depth stage */

    void setSecondaryStage(int secondaryStage); /* My Change: added a secondary depth stage */

    public static enum TestFunction {
        ALWAYS_FAIL,
        ALWAYS_PASS,
        EQUAL,
        NOT_EQUAL,
        LESS,
        LESS_OR_EQUAL,
        GREATER,
        GREATER_OR_EQUAL;

        private TestFunction() {
        }
    }
}
