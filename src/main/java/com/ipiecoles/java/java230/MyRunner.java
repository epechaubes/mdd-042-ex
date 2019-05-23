package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) {
        String fileName = "employes.csv";
        readFile(fileName);

        employeRepository.save(employes);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) {
        Stream<String> stream;
        logger.info("Lecture du fichier : " + fileName);
        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
        } catch (IOException e) {
            logger.error("Problème dans l'ouverture du fichier : " + fileName);
            return new ArrayList<>();
        }
        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size() + " lignes lues");
        for (int i=0; i<lignes.size(); i++){
            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {
                logger.error("Ligne " + (i+1) + " : " + e.getMessage() + " => " + lignes.get(i));
            }
        }

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        String grade = ligne.substring(0,1);
        switch(grade){
            case "C" :
                processCommercial(ligne);
                break;
            case "M" :
                processManager(ligne);
                break;
            case "T" :
                processTechnicien(ligne);
                break;
            default :
                throw new BatchException("Type d'employé inconnu : " + grade);
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        String[] infoEmploye = ligneCommercial.split(",");

        if (infoEmploye.length == 7){

            Commercial c = new Commercial();

            try {
                c.setCaAnnuel(Double.parseDouble(infoEmploye[5]));
            } catch (Exception e){
                throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + infoEmploye[5]);
            }

            try {
                c.setPerformance(Integer.parseInt(infoEmploye[6]));
            } catch (Exception e){
                throw new BatchException("La performance du commercial est incorrecte : " + infoEmploye[6]);
            }

            processEmploye(c, infoEmploye);

            employes.add(c);

        } else {
            throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + infoEmploye.length);
        }
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        String[] infoEmploye = ligneManager.split(",");

        if (infoEmploye.length == 5){

            Manager m = new Manager();

            processEmploye(m, infoEmploye);

            employes.add(m);

        } else {
            throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + infoEmploye.length);
        }
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        String[] infoEmploye = ligneTechnicien.split(",");

        if (infoEmploye.length == 7){

            Technicien t = new Technicien();

            try{
                t.setGrade(Integer.parseInt(infoEmploye[5]));
            } catch (TechnicienException e) {
                throw new BatchException(e.getMessage());
            } catch (Exception e) {
                throw new BatchException("Le grade du technicien est incorect : " + infoEmploye[5]);
            }


            if(infoEmploye[6].matches("^[MTC][0-9]{5}$")){
                ArrayList<String> matricules = new ArrayList<>();
                for (Employe emp : employes) {
                    if (emp instanceof Manager){
                        matricules.add(emp.getMatricule());
                    }
                }
                if (managerRepository.findByMatricule(infoEmploye[6]) != null || matricules.contains(infoEmploye[6])) {
                    t.setManager(managerRepository.findByMatricule(infoEmploye[6]));
                } else {
                    throw new BatchException("Le manager de matricule "+ infoEmploye[6] +" n'a pas été trouvé dans le fichier ou en base de données ");
                }
            }else{
                throw new BatchException("la chaîne " + infoEmploye[6] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
            }

            processEmploye(t, infoEmploye);

            employes.add(t);

        } else {
            throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + infoEmploye.length);
        }
    }

    /**
     * Méthode qui crée un Employé à partir d'une ligne contenant les informations d'un d'un employé
     * @param infoEmploye la ligne contenant les infos de l'employé à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processEmploye(Employe emp, String infoEmploye[]) throws BatchException {

        if (infoEmploye[0].matches("^[MTC][0-9]{5}$")){
            emp.setMatricule(infoEmploye[0]);
        } else {
            throw new BatchException("La chaîne " + infoEmploye[0] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$ ");
        }

        emp.setNom(infoEmploye[1]);

        emp.setPrenom(infoEmploye[2]);

        try {
            emp.setDateEmbauche(DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(infoEmploye[3]));
        } catch (Exception e) {
            throw new BatchException(infoEmploye[3] + " ne respecte pas le format de date dd/MM/yyyy ");
        }

        try {
            emp.setSalaire(Double.parseDouble(infoEmploye[4]));
        } catch (Exception e){
            throw new BatchException(infoEmploye[4] + " n'est pas un nombre valide pour un salaire ");
        }
    }

}
