package org.wikipathways.cytoscapeapp.impl.gpml;


public enum MIM
{
	MIM_NECESSARY_STIMULATION("mim-necessary-stimulation", "Arrow"),
    MIM_BINDING("mim-binding", "Arrow"),
    MIM_CONVERSION("mim-conversion", "Arrow"),
    MIM_STIMULATION("mim-stimulation", "Arrow"),
    MIM_MODIFICATION("mim-modification", "Arrow"),
    MIM_CATALYSIS("mim-catalysis", "Arrow"),
    MIM_INHIBITION("mim-inhibition", "tbar"),
    MIM_CLEAVAGE("mim-cleavage", "Arrow"),
    MIM_COVALENT_BOND("mim-covalent-bond", "Arrow"),
    MIM_BRANCHING_LEFT("mim-branching-left", null),
    MIM_BRANCHING_RIGHT("mim-branching-right", null),
    MIM_TRANSLATION("mim-transcription-translation", "HalfCircle"),
    MIM_GAP("mim-gap", null);

	private String id;
	private String type;
	
	MIM(String name, String arrow)
	{
		id = name;
		type = arrow;
	}

	public String getDescription()
	{
		String cleaner = id.substring(4).replace("-", " ");
		return StringUtil.capitalize(cleaner);
	}
	static public MIM lookup(String name)
	{
		for (MIM m : values())
			if (m.id.equals(name)) return m;
		return MIM_BINDING;
	}
	public String getId() { return id; }

	
}
