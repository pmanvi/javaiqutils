import java.io.Externalizable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
/**
 * Sample code showing how forward compatibility of serialized objects.
 * Since this Proof Of Concept, user is suppose to make code changes and 
 * understand :-)
 */
public class ForwardCompatibleSerializedObjectPOC {
  
    public static void main(String[] args) throws Exception {
        FileOutputStream fs = new FileOutputStream("TESTVERSION_0.ser");
        ObjectOutputStream oos = new ObjectOutputStream(fs);
        oos.writeObject(new Test());
        oos.flush();
        oos.close();
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream("TESTVERSION_0.ser"));
        Thread.sleep(1000);
        System.out.println(ois.readObject());
        
    }
    
}
// Marker interface class used to mark the start and end of the new data that's getting 
// added in newer versions.
final class SKIP_START implements Serializable{
    public SKIP_START(){}
}
final class SKIP_END implements Serializable{
    public SKIP_END(){}
}

// Representing a evolvable unit that will be persisted in different versions
class Test implements Externalizable {

    @Override
    public String toString() {
        return "Test{" + "VERSION=" + VERSION + ", number=" + number + ", name=" + name;// + ", version1Data=" + version1Data + '}';
    }
    private static final long serialVersionUID = 1L;
    
    public Test(){
      number=10;
      name="praveen";
      //version1Data="version1Data";
    }
   
    int VERSION=0;
    int number;
    String name;
    //String version1Data;
    //String version2Data;

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
       out.writeInt(VERSION);
       out.writeInt(number);
       //Added for version1
       //out.writeObject(new SKIP_START()); 
       //out.writeObject(version1Data);
       //out.writeObject(new SKIP_END()); 
       out.writeObject(name);
       
    }

    @Override
    public void readExternal(ObjectInput _in) throws IOException, ClassNotFoundException {
        ObjectInputDecorator in = new ObjectInputDecorator(_in);
        VERSION = in.readInt();
        number = in.readInt();
        //if(VERSION>=1) {
            //in.readObject();
            //version1Data = (String)in.readObject();
            //in.readObject();
        //}
        name = (String) in.readObject();
    }
    
    // This class implements all the methods ObjectInput & moves the cursor
    // whenever it sees skippable data from future versions
    // WARNING : Currently for POC only 2 methods are shown
    class ObjectInputDecorator {
        ObjectInput input;
        ObjectInputDecorator(ObjectInput input){
            this.input=input;
        }
        
         public int readInt() throws IOException, ClassNotFoundException{
            return new ForwardCompatiableContainer<Integer>(input){
                @Override
                public Integer get() throws IOException, OptionalDataException {
                    return input.readInt();
                }
             }.execute();
         }
        
         public Object readObject() throws IOException, ClassNotFoundException{
            return new ForwardCompatiableContainer<Object>(input){
                @Override
                public Object get() throws ClassCastException, IOException, OptionalDataException,ClassNotFoundException {
                    return input.readObject();
                }
             }.execute();
         }
        
         abstract class ForwardCompatiableContainer<T> {
             private ObjectInput objectInput;
             public ForwardCompatiableContainer(final ObjectInput decorator ){
                   this.objectInput=decorator;
             }
             public final T execute() throws IOException,ClassNotFoundException{
                     T t= get();
                     if(t instanceof SKIP_START){
                         while(true){
                             //System.out.println("Reading till we get skip end...");
                             try{
                                SKIP_END skipEnd = (SKIP_END) objectInput.readObject();
                                // Ok we skipped all unwanted stuff, as there was no exception
                                t= get();
                                break;

                             }catch(ClassCastException exp){
                                 //exp.printStackTrace();
                                 //Its expected to get ClassCastException till 
                                 //new information is skipped.
                             }catch(IOException exp){
                                 throw exp;
                             }catch(ClassNotFoundException exp){
                                 throw exp;
                             }
                         }
         
                     }
                 return t;
             }
             public abstract T get() throws IOException,ClassNotFoundException;
         }
    }
        
}
