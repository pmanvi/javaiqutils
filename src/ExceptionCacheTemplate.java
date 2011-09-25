import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class can be used at the front end to cache the exception for recurring
 * exceptions for similar kind of input data and there is fair chance that condition
 * throw exception back is conclusively predictable for any given amount of time.
 * <br/>
 * It provides a container for time/resource heavy operations that result in
 * recurring similar errors that can be cached for a given interval of time.
 * This helps to reduce the load on the server and improve the efficiency. <br/>
 * Usage : <br/>
 * 
 * <p><blockquote><pre>
 *         return new ExceptionCacheTemplate<Integer,FileNotFoundException>(){
 *              @Override
 *              public Integer handle() throws FileNotFoundException {
 *               // APPLICATION CODE throwing FileNotFoundExcetption
 *                   Thread.sleep(3000); ////simulating some heavy operation
 *                   if(true) throw new FileNotFoundException("Checked Exception");
 *                    return 10;
 *                  }
 *                // APPLICATION CODE ENDS
 *         }.runIn(new ExceptionKey("StringObject",30000));
  * </pre></blockquote>
 * <p>
 */
public abstract class ExceptionCacheTemplate<T,E extends Exception>{
    
    private static Map<ExceptionKey,Exception>  expCacheMap = null;
    
    
    public abstract T handle() throws Exception;
   
    public final T runIn(final String key) throws E {
          return runIn(new ExceptionKey(key,30000));
    }
    
    public final T runIn(final ExceptionKey key) throws E{
        try{
           if(expCacheMap==null){
               E e= (E)getCache().get(key);
               if(e!=null){
                   throw e;
               }
           } else {
               // Default implementation in place
               if(expCacheMap.containsKey(key)){
                   if(key.isExpired()){
                      expCacheMap.remove(key); 
                   }else{
                      throw expCacheMap.get(key);
                   }
               } 
           }
           return handle();
        }catch(Exception e){
            try{
                E originalException =  (E) e;
                getCache().put(key, originalException);
                throw originalException;
            }catch(ClassCastException cls){
                // We got Exception other than E
                // So if user implements the method and that translates well & good
                // Else we will just throw back & don't cache.
                final E translatedException = translate(e);
                if(translatedException!=null){
                    getCache().put(key, translatedException);
                    throw translatedException;
                }
            }
          throw new RuntimeException("UnExpected Exception "+e.toString(),e);
        }
    }
    
    /**
     * Optional method to override,This can be used to translate the
     * Exception to the required exception. This will make sure that
     * if any Exception other than 'E' will also be cached.
     * @param e  EXCEPTION
     * @return 
     */
    public E translate(Exception e){
        return null;
    }
    /**
     * Optional method to override, Different Cache implementation could be
     * provided
     * @return
     */
    public Map<ExceptionKey,Exception> getCache(){
        if(expCacheMap==null){
            expCacheMap = new ConcurrentHashMap<ExceptionKey,Exception>();
        }
        return expCacheMap;
    }
    
    /**
     * A helper utility class holding the information that resulted in exception
     * @param <K> 
     */
    public static class ExceptionKey<K> {

        private long currentTimeStamp = 0l;
        private K key;
        private long expiryTime;
        
        public ExceptionKey(K key,long allowedTime){
            this.key = key;
            currentTimeStamp = System.currentTimeMillis();
            this.expiryTime=allowedTime;
        }
        
       
        private K getKey(){
            return key;
        }
        private boolean isExpired(){
            return System.currentTimeMillis()-currentTimeStamp > this.expiryTime;
        }
        @Override
        public boolean equals(final Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ExceptionKey other = (ExceptionKey) obj;
            if ((this.key == null) ? (other.key != null) : !this.key.equals(other.key)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + (this.key != null ? this.key.hashCode() : 0);
            return hash;
        }
    }
   
      
}


