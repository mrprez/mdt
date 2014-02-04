package com.mrprez.gencross.impl.mdt;

import java.util.Iterator;
import java.util.List;

import com.mrprez.gencross.Personnage;
import com.mrprez.gencross.PoolPoint;
import com.mrprez.gencross.Property;
import com.mrprez.gencross.history.ConstantHistoryFactory;
import com.mrprez.gencross.history.HistoryItem;
import com.mrprez.gencross.history.HistoryUtil;
import com.mrprez.gencross.history.LevelToReachHistoryFactory;
import com.mrprez.gencross.value.Value;

public class MdT extends Personnage {

	
	@Override
	public void calculate() {
		super.calculate();
		if(phase.equals("Creation")){
			calculateAttributs();
			calculateTalents();
			calculatePointPools();
			calculateViceEtVertu();
			calculateAvantages();
		}
	}
	
	private void calculateViceEtVertu(){
		if(getProperty("Vice").getValue().toString().equals("") || getProperty("Vertu").getValue().toString().equals("")){
			errors.add("Vous devez choisir une vertu et un vice");
		}
	}
	
	private void calculatePointPools(){
		String error = null;
		Iterator<PoolPoint> it = getPointPools().values().iterator();
		while(it.hasNext() && error==null){
			if(it.next().getRemaining()!=0){
				error = "Il reste des points à dépenser";
			}
		}
		if(error!=null){
			errors.add(error);
		}
	}
	
	private void calculateAttributs(){
		String mental = getProperty("Attributs#Mental").getValue().toString();
		String physique = getProperty("Attributs#Physique").getValue().toString();
		String social = getProperty("Attributs#Social").getValue().toString();
		if(mental.equals(physique) || mental.equals(social) || physique.equals(social)){
			errors.add("Vous devez hiérarchiser vos groupes d'Attributs");
		}
	}
	
	private void calculateTalents(){
		String mental = getProperty("Talents#Mental").getValue().toString();
		String physique = getProperty("Talents#Physique").getValue().toString();
		String social = getProperty("Talents#Social").getValue().toString();
		if(mental.equals(physique) || mental.equals(social) || physique.equals(social)){
			errors.add("Vous devez hiérarchiser vos groupes de Talents");
		}
	}
	
	private void calculateAvantages(){
		for(Property avantage : getProperty("Avantages").getSubProperties().getProperties().values()){
			String errorMessage = checkAvantage(avantage);
			if(errorMessage!=null){
				errors.add(avantage.getFullName()+": "+errorMessage);
			}
		}
	}
	
	public void changeAttributGroupValue(Property attributGroup, Value oldValue){
		String newPoolPoint = "Attributs "+attributGroup.getValue().toString();
		String oldPoolPoint = "Attributs "+oldValue.toString();
		int transfertCost = 0;
		for(Property attribut : attributGroup.getSubProperties().getProperties().values()){
			attribut.getHistoryFactory().setPointPool(newPoolPoint);
			List<HistoryItem> subHistory = HistoryUtil.getSubHistory(history, attribut);
			for(HistoryItem subHistoryItem : subHistory){
				transfertCost = transfertCost + subHistoryItem.getCost();
				subHistoryItem.setPointPool(newPoolPoint);
			}
		}
		getPointPools().get(oldPoolPoint).spend(-transfertCost);
		getPointPools().get(newPoolPoint).spend(transfertCost);
	}
	
	public void changeTalentGroupValue(Property talentGroup, Value oldValue){
		String newPoolPoint = "Talents "+talentGroup.getValue().toString();
		String oldPoolPoint = "Talents "+oldValue.toString();
		int transfertCost = 0;
		for(Property talent : talentGroup.getSubProperties().getProperties().values()){
			talent.getActualHistoryFactory().setPointPool(newPoolPoint);
			List<HistoryItem> subHistory = HistoryUtil.getSubHistory(history, talent);
			for(HistoryItem subHistoryItem : subHistory){
				transfertCost = transfertCost + subHistoryItem.getCost();
				subHistoryItem.setPointPool(newPoolPoint);
			}
		}
		getPointPools().get(oldPoolPoint).spend(-transfertCost);
		getPointPools().get(newPoolPoint).spend(transfertCost);
	}
	
	public Boolean addAvantage(Property avantage){
		String erreur = checkAvantage(avantage);
		if(erreur!=null){
			actionMessage = erreur;
			return false;
		}
		return true;
	}
	
	public void creationFinished(){
		pointPools.put("Experience", new PoolPoint("Experience", 0));
		
		Property attributs = getProperty("Attributs");
		for(Property attributGroup : attributs.getSubProperties()){
			attributGroup.setEditable(false);
			for(Property attribut : attributGroup.getSubProperties()){
				attribut.setHistoryFactory(new LevelToReachHistoryFactory(5, "Experience"));
			}
		}
		Property talents = getProperty("Talents");
		for(Property talentGroup : talents.getSubProperties()){
			talentGroup.setEditable(false);
			for(Property talent : talentGroup.getSubProperties()){
				talent.setHistoryFactory(new LevelToReachHistoryFactory(3, "Experience"));
				talent.getSubProperties().getDefaultProperty().setHistoryFactory(new ConstantHistoryFactory("Experience", 3));
			}
		}
		Property avantages = getProperty("Avantages");
		avantages.getSubProperties().getDefaultProperty().setHistoryFactory(new LevelToReachHistoryFactory(2, "Expérience"));
		for(Property avantage : avantages.getSubProperties()){
			avantage.setHistoryFactory(new LevelToReachHistoryFactory(2, "Experience"));
		}
		for(Property option : avantages.getSubProperties().getOptions().values()){
			option.setHistoryFactory(new LevelToReachHistoryFactory(2, "Experience"));
		}
		Property moralite = getProperty("Moralité");
		moralite.setEditable(true);
		moralite.setHistoryFactory(new LevelToReachHistoryFactory(3, "Experience"));
		
	}
	
	private int compteLangues(Property langue){
		int compte = 0;
		for(Property avantage : getProperty("Avantages").getSubProperties().getProperties().values()){
			if(avantage.getName().equals("Langue étrangère")){
				compte++;
			}
		}
		if(getProperty("Avantages").getSubProperty(langue.getFullName())==null){
			compte++;
		}
		return compte;
	}
	
	private String checkAvantage(Property avantage){
		if(avantage.getName().equals("Langue étrangère")){
			if(compteLangues(avantage)>getProperty("Attributs#Mental#Intelligence").getValue().getInt()){
				return "Vous ne pouvez avoir plus de Langues étrangères que votre Intelligence";
			}
		}
		if(avantage.getName().equals("Conscience de l’invisible")){
			if(getProperty("Attributs#Mental#Astuce").getValue().getInt()<2){
				return "Vous devez avoir Astuce à 2";
			}
		}
		if(avantage.getName().equals("Capacité pulmonaire")){
			if(getProperty("Talents#Physique#Athlétisme").getValue().getInt()<3){
				return "Vous devez avoir Athlétisme à 3";
			}
		}
		if(avantage.getName().equals("Cascadeur")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
		}
		if(avantage.getName().equals("Désarmer")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
			if(getProperty("Talents#Physique#Mélée").getValue().getInt()<2){
				return "Vous devez avoir Mélée à 2";
			}
		}
		if(avantage.getName().equals("Dos musclé")){
			if(getProperty("Attributs#Physique#Force").getValue().getInt()<2){
				return "Vous devez avoir Force à 2";
			}
		}
		if(avantage.getName().equals("Esquive (armes blanches)")){
			if(getProperty("Attributs#Physique#Force").getValue().getInt()<2){
				return "Vous devez avoir Force à 2";
			}
			if(getProperty("Talents#Physique#Mélée").getValue().getInt()<1){
				return "Vous devez avoir Mélée à 1";
			}
		}
		if(avantage.getName().equals("Esquive (mains nues)")){
			if(getProperty("Attributs#Physique#Force").getValue().getInt()<2){
				return "Vous devez avoir Force à 2";
			}
			if(getProperty("Talents#Physique#Bagarre").getValue().getInt()<1){
				return "Vous devez avoir Bagarre à 1";
			}
		}
		if(avantage.getName().equals("Estomac en béton")){
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<2){
				return "Vous devez avoir Vigueur à 2";
			}
		}
		if(avantage.getName().equals("Flingueur")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
			if(getProperty("Talents#Physique#Armes à feu").getValue().getInt()<3){
				return "Vous devez avoir Armes à feu à 3";
			}
		}
		if(avantage.getName().equals("Guérison rapide")){
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<4){
				return "Vous devez avoir Vigueur à 4";
			}
		}
		if(avantage.getName().equals("Immunité innée")){
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<2){
				return "Vous devez avoir Vigueur à 2";
			}
		}
		if(avantage.getName().equals("Réflexes rapides")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
		}
		if(avantage.getName().equals("Résistance aux toxines")){
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<3){
				return "Vous devez avoir Vigueur à 3";
			}
		}
		if(avantage.getName().equals("Santé de fer")){
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<3
					&& getProperty("Attributs#Physique#Résolution").getValue().getInt()<3){
				return "Vous devez avoir Vigueur ou Résolution à 3";
			}
		}
		if(avantage.getName().equals("Sprinteur")){
			if(getProperty("Attributs#Physique#Force").getValue().getInt()<2){
				return "Vous devez avoir Force à 2";
			}
		}
		if(avantage.getName().equals("Style de combat : boxe")){
			if(getProperty("Attributs#Physique#Force").getValue().getInt()<3){
				return "Vous devez avoir Force à 3";
			}
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<2){
				return "Vous devez avoir Vigueur à 2";
			}
			if(getProperty("Talents#Physique#Bagarre").getValue().getInt()<2){
				return "Vous devez avoir Bagarre à 2";
			}
		}
		if(avantage.getName().equals("Style de combat : Kung-fu")){
			if(getProperty("Attributs#Physique#Force").getValue().getInt()<2){
				return "Vous devez avoir Force à 2";
			}
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<2){
				return "Vous devez avoir Agilité à 2";
			}
			if(getProperty("Attributs#Physique#Vigueur").getValue().getInt()<2){
				return "Vous devez avoir Vigueur à 2";
			}
			if(getProperty("Talents#Physique#Bagarre").getValue().getInt()<2){
				return "Vous devez avoir Bagarre à 2";
			}
		}
		if(avantage.getName().equals("Style de combat : deux armes blanches")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
			if(getProperty("Talents#Physique#Mélée").getValue().getInt()<3){
				return "Vous devez avoir Mélée à 3";
			}
		}
		if(avantage.getName().equals("Techniques de combat")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
			if(getProperty("Talents#Physique#Mélée").getValue().getInt()<2){
				return "Vous devez avoir Mélée à 2";
			}
		}
		if(avantage.getName().equals("Tir rapide")){
			if(getProperty("Attributs#Physique#Agilité").getValue().getInt()<3){
				return "Vous devez avoir Agilité à 3";
			}
		}
		if(avantage.getName().equals("Tour de chauffe")){
			if(getProperty("Avantages#Réflexes rapides")==null || getProperty("Avantages#Réflexes rapides").getValue().getInt()<2){
				return "Vous devez avoir Réflexes rapides à 2";
			}
		}
		if(avantage.getName().equals("Inspiration")){
			if(getProperty("Attributs#Social#Présence").getValue().getInt()<4){
				return "Vous devez avoir Présence à 4";
			}
		}
		if(avantage.getName().equals("Renommée")){
			if(getProperty("Attributs#Mental#Astuce").getValue().getInt()<5){
				return "Vous devez avoir Astuce à 5";
			}
		}
		return null;
	}
	
	

}
