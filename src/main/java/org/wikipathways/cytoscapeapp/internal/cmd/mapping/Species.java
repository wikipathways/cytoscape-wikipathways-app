package org.wikipathways.cytoscapeapp.internal.cmd.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// this is a partial list of the most commonly studied species
// see http://webservice.bridgedb.org/contents for full list supported in BridgeDB
public enum Species {
	   Human("Human", "Homo sapiens") ,
	   Mouse("Mouse", "Mus musculus") ,
	   Rat("Rat", "Rattus norvegicus") ,
	   Frog("Frog", "Xenopus tropicalis") ,
	   Zebra_fish("Zebra fish", "Danio rerio") ,
	   Fruit_fly("Fruit fly", "Drosophila melanogaster") ,
	   Mosquito("Mosquito", "Anopheles gambiae") ,
	   Arabidopsis_thaliana("Arabidopsis thaliana", "Arabidopsis thaliana") ,
	   Yeast("Yeast", "Saccharomyces cerevisiae") ,
	   Escherichia_coli("E. coli", "Escherichia coli") ,
	   Tuberculosis("Tuberculosis", "Mycobacterium tuberculosis") ,
	   Worm("Worm", "Caenorhabditis elegans");
 
	private String name;
	private String latin;

	static Map<String, String> mapToCommon;
	static Map<String, String> mapToLatin;
	//--------------------------------------------------------------------
	
	private Species(String commonName, String latinName)
	{
		name = commonName;
		latin = latinName;
	}
	
	public static void buildMaps()
	{
		if (mapToCommon == null)
		{
			mapToCommon = new HashMap<String, String>();
			mapToLatin = new HashMap<String, String>();
			for (Species spec : values())
			{
				mapToCommon.put(spec.latin, spec.name);
				mapToLatin.put(spec.name, spec.latin);
			}
		}
	}
	
	public String common()		{ return name;		}
	public String latin()		{ return latin;		}
	public String fullname() 	{ return name + " (" + latin + ")";	}
	//--------------------------------------------------------------------
	public static Species lookup(String input)
	{
		if (input == null || input.trim().length() == 0 ) return null;
		int idx = input.indexOf(" (");
		if (idx > 0) input = input.substring(0,idx);
		for (Species s : values())
			if (s.toString().compareToIgnoreCase(input) == 0 || s.name.compareToIgnoreCase(input) == 0 || s.latin.compareToIgnoreCase(input) == 0)
				return s;
		return null;
	}
	//--------------------------------------------------------------------
	public static String[] commonNames()
	{
		String[] names = new String[Species.values().length];
		int i = 0;
		for (Species spec : values())
			names[i++] = spec.name;
		return names;		
	}
	
	public static String[] latinNames()
	{
		String[] latinnames = new String[Species.values().length];
		int i = 0;
		for (Species spec : values())
			latinnames[i++] = spec.latin;
		return latinnames;		
	}
	
	public static String[] fullNameArray()
	{
		String[] fullNames = new String[Species.values().length];
		int i = 0;
		for (Species spec : values())
			fullNames[i++] = spec.fullname();
		return fullNames;		
	}
	
	public static List<String> fullNames()
	{
		List<String> fullNames = new ArrayList<String>();
		for (Species spec : values())
			fullNames.add(spec.fullname());
		return fullNames;		
	}

	public boolean equals(String other)
	{
		if ( name.equals(other)) return true;
		if ( latin.equals(other)) return true;
		if ( fullname().equals(other)) return true;
		if ( toString().equals(other)) return true;
		return false;
	}
	
	public boolean match(Species other)
	{
		if (other == null || other.name().trim().length() == 0) return true;	// match 
		return (name.equals(other.name));
	}
	
	public boolean isHuman()	{		return (match(Species.Human));	}
	public boolean isFly()		{		return (match(Species.Fruit_fly));	}
	public boolean isYeast()	{		return (match(Species.Yeast));	}
	public boolean isWorm()		{		return (match(Species.Worm));	}
	public boolean isMouse()	{		return (match(Species.Mouse));	}
}
