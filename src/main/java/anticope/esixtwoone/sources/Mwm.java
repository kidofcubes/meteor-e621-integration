package anticope.esixtwoone.sources;

import java.util.ArrayList;
import java.util.List;

public class Mwm extends Source{

    private final String domain;
    private final List<String> paths;
    public Mwm(String domain, List<String> paths){
        this.domain = domain;
        this.paths=new ArrayList<>(paths);
    }
    @Override
    public void reset() {}

    @Override
    protected String randomImage(String filter, Size size) {
        if(paths.contains(filter)){
            return String.format("%s/%s/", domain, filter);
        }else{
            return String.format("%s/%s/", domain, paths.get(0));
        }
    }
}
