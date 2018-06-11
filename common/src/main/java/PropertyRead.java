import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class PropertyRead {
    private String pathToProperty;
    private Properties property;

    public PropertyRead(String pathToProperty) {
        this.pathToProperty = pathToProperty;
        this.property = readSetProperty ();
        }

    private Properties readSetProperty (){
        System.out.println("Read Property");
        Properties property = new Properties();
            try (FileInputStream fis = new FileInputStream(pathToProperty)){
                property.load(fis);
            } catch (IOException e) {
                System.err.println("Error: file properties not exist!");
                e.printStackTrace();
            }
        return property;
    }

    public String getProperty(String key) {
        return property.getProperty(key);
    }

    public void setProperty(String key, String value) {
        Properties property = new Properties();
        try (FileOutputStream fos = new FileOutputStream(pathToProperty)){
            property.setProperty(key, value);
            property.store(fos, null);
        } catch (IOException e) {
            System.err.println("Error: file properties not exist!");
            e.printStackTrace();
        }
    }
}
