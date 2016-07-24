package games.strategy.engine.message.unifiedmessenger;

import games.strategy.engine.message.RemoteMethodCall;
import games.strategy.net.GUID;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

// someone wants us to invoke something locally
public abstract class Invoke implements Externalizable {
  private static final long serialVersionUID = -5453883962199970896L;
  public GUID methodCallID;
  public boolean needReturnValues;
  public RemoteMethodCall call;

  public Invoke() {}

  @Override
  public String toString() {
    return "invoke on:" + call.getRemoteName() + " method name:" + call.getMethodName() + " method call id:"
        + methodCallID;
  }

  public Invoke(final GUID methodCallID, final boolean needReturnValues, final RemoteMethodCall call) {
    if (needReturnValues && methodCallID == null) {
      throw new IllegalArgumentException("Cant have no id and need return values");
    }
    if (!needReturnValues && methodCallID != null) {
      throw new IllegalArgumentException("Cant have id and not need return values");
    }
    this.methodCallID = methodCallID;
    this.needReturnValues = needReturnValues;
    this.call = call;
  }

  @Override
  public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException {
    needReturnValues = in.read() == 1;
    if (needReturnValues) {
      methodCallID = (GUID) in.readObject();
    }
    call = new RemoteMethodCall();
    call.readExternal(in);
  }

  @Override
  public void writeExternal(final ObjectOutput out) throws IOException {
    out.write(needReturnValues ? 1 : 0);
    if (needReturnValues) {
      out.writeObject(methodCallID);
    }
    call.writeExternal(out);
  }
}