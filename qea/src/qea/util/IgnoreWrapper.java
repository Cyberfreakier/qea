package qea.util;

public interface IgnoreWrapper<K> {

	public void ignore(K key);
	public boolean isIgnored(K key);

}
