package com.ipiccie.wari.Screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.MapLayers;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.maps.tiled.tiles.StaticTiledMapTile;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Pool;
import com.ipiccie.wari.ActiviteduJeu;

public class EcranInfini extends ScreenAdapter {
    ActiviteduJeu jeu;
    private TiledMap carte;
    private MapLayers layers;
    private static final float GRAVITE = -12;
    private static final float LATERAL_GRAVITE = 20;
    private static final float BONUS_X = 5;
    private static final float VAL_IMPULSION_SAUT = 1000;
    private static final float VITESSE_MAX_X = 230;
    private static final float VITESSE_MAX_Y = 320;
    private float MONDE_LARGEUR;
    private float MONDE_HAUTEUR;
    private float PERSO_DEBUT_X;
    private float PERSO_DEBUT_Y;
    private int echelle;
    private OrthogonalTiledMapRenderer rendu;
    private OrthographicCamera camera;
    private EcranJeux.Perso personnage;
    private Music laMusiqueTropB1;
    private TextureRegion versMenu;
    private TextureRegion rejouer;
    private TextureRegion Mpause;
    private TextureRegion Epause;
    private TextureRegion pret;
    private Animation<TextureRegion> attend;
    private Animation<TextureRegion> marche;
    private Animation<TextureRegion> saute;
    private Animation<TextureRegion> attendT;
    private Animation<TextureRegion> marcheT;
    private Animation<TextureRegion> sauteT;
    private BitmapFont police;
    private Preferences prefs;
    Vector2 vecteurGravite = new Vector2();
    float etatTemporel = 0;
    EtatDuJeu etatDuJeu = EtatDuJeu.Lancement;
    private final Pool<Rectangle> piscineRectangle = new Pool<Rectangle>() {
        @Override
        protected Rectangle newObject() {
            return  new Rectangle();
        }
    };


    public EcranInfini(ActiviteduJeu jeu){
        this.jeu = jeu;
    }

    @java.lang.Override
    public void show() {
        Gdx.app.setLogLevel(Application.LOG_DEBUG);
        prefs = Gdx.app.getPreferences("Parametres");
        police = new BitmapFont();
        police.getData().setScale(2F);
        carte = new TiledMap();
        layers = carte.getLayers();
        float largeur = Gdx.graphics.getWidth();
        float hauteur = Gdx.graphics.getHeight();
        TiledMapTileLayer calque1 = new TiledMapTileLayer(544, 20, 32, 32);
        calque1.setName("sol");
        echelle = calque1.getTileWidth();
        MONDE_HAUTEUR = calque1.getHeight();
        MONDE_LARGEUR = calque1.getWidth();
        PERSO_DEBUT_Y = echelle * 3F;
        TiledMapTileLayer calque2 = new TiledMapTileLayer(calque1.getWidth(), calque1.getHeight(), echelle, echelle);
        calque2.setName("obstacles");
        camera = new OrthographicCamera();
        camera.setToOrtho(false,15*echelle*(largeur / hauteur),15F * echelle);
        //creerCarte(calque1, calque2,(int)(camera.viewportWidth/echelle));
        layers.add(calque1);
        layers.add(calque2);
        rendu = new OrthogonalTiledMapRenderer(carte, 1); //met ?? l'??chelle de la carte--> 1 unit?? = X px
        personnage = new EcranJeux.Perso();
        Texture notreHeros = new Texture("notre_heros.png");
        Texture notreHerosAvecStyle = new Texture("perso_truc_trainee.png");
        TextureRegion[] regions = TextureRegion.split(notreHeros,200,280)[prefs.getInteger("perso",0)];
        TextureRegion[] regionsT = TextureRegion.split(notreHerosAvecStyle,30,20)[prefs.getInteger("traine",0)];
        attend = new Animation<>(0,regions[7]);
        marche = new Animation<>(0.1F,regions[0],regions[1],regions[2],regions[3],regions[4],regions[5]);
        saute = new Animation<>(0, regions[6]);
        attendT = new Animation<>(0,regionsT[3]);
        marcheT = new Animation<>(0.1F,regionsT[0],regionsT[1],regionsT[2]);
        sauteT = new Animation<>(0, regionsT[3]);
        marche.setPlayMode(Animation.PlayMode.LOOP);
        marcheT.setPlayMode(Animation.PlayMode.LOOP_PINGPONG);
        EcranJeux.Perso.HEIGHT = 1.8F * echelle;
        EcranJeux.Perso.WIDTH = 1.8F * echelle;
        versMenu = new TextureRegion(new Texture("icons.png"),32,32);
        rejouer = TextureRegion.split(new Texture("icons.png"),32,32)[1][0];
        Epause = TextureRegion.split(new Texture("icons.png"),32,32)[5][0];
        Mpause = TextureRegion.split(new Texture("icons.png"),32,32)[6][0];

        pret = new TextureRegion(new Texture("pret.png"));
        PERSO_DEBUT_X = camera.viewportWidth / 3;
        leGrandReset();
    }


    @java.lang.Override
    public void render(float delta) {
        Gdx.gl.glClearColor(1,0,0,1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        maJjeu();
        camera.update();
        rendu.setView(camera);
        rendu.render();
        dessineJeu();
    }


    public void dessineJeu(){
        jeu.batch.setProjectionMatrix(camera.combined);
        jeu.batch.begin();
        renduPerso();
        if (etatDuJeu == EtatDuJeu.Finissant){
            jeu.batch.draw(rejouer,camera.position.x-3*echelle,camera.position.y-3 * echelle,2F*echelle,2F*echelle);
            jeu.batch.draw(versMenu,camera.position.x+echelle,camera.position.y-3 * echelle,2F*echelle,2F*echelle);
            police.draw(jeu.batch,"score: "+(int)((personnage.position.x-PERSO_DEBUT_X)/echelle),camera.position.x-3*echelle,camera.position.y+echelle);
            police.draw(jeu.batch,"meilleur score: "+prefs.getInteger("score",(int)((personnage.position.x-PERSO_DEBUT_X)/echelle)),camera.position.x-3*echelle,camera.position.y);
        }else if (etatDuJeu == EtatDuJeu.Pause){
            jeu.batch.draw(rejouer,camera.position.x- 4*echelle,camera.position.y-3 * echelle,2F*echelle,2F*echelle);
            jeu.batch.draw(versMenu,camera.position.x+ 2 * echelle,camera.position.y-3 * echelle,2F*echelle,2F*echelle);
            jeu.batch.draw(Epause, camera.position.x-echelle,camera.position.y-3 * echelle,2F*echelle,2F*echelle);
        } else{
            police.draw(jeu.batch,"score: "+(int)((personnage.position.x-PERSO_DEBUT_X)/echelle),echelle+(camera.position.x-camera.viewportWidth/2F),camera.position.y-echelle+camera.viewportHeight/2F);
            if(etatDuJeu == EtatDuJeu.Lancement){
                jeu.batch.draw(pret,camera.position.x - pret.getRegionWidth()/2F,camera.position.y - pret.getRegionHeight());
            }else{
                jeu.batch.draw(Mpause,camera.position.x + camera.viewportWidth/2F - 2 * echelle,camera.position.y-2*echelle+camera.viewportHeight/2F,echelle,echelle);
            }
        }
        jeu.batch.end();
    }

    public void renduPerso(){
        TextureRegion img  = null;
        TextureRegion img2  = null;
        switch (personnage.etat){
            case Attend:
                img = attend.getKeyFrame(personnage.etatTemporel);
                img2 = attendT.getKeyFrame(personnage.etatTemporel);
                break;
            case Marche:
                img = marche.getKeyFrame(personnage.etatTemporel);
                img2 = marcheT.getKeyFrame(personnage.etatTemporel);
                break;
            case Vole:
                img = saute.getKeyFrame(personnage.etatTemporel);
                img2 = sauteT.getKeyFrame(personnage.etatTemporel);
                break;
            default:
                break;
        }
        //jeu.batch.draw(img2,personnage.position.x - EcranJeux.Perso.WIDTH/2,personnage.position.y, EcranJeux.Perso.WIDTH*3/2, EcranJeux.Perso.HEIGHT);
        jeu.batch.draw(img,personnage.position.x,personnage.position.y, EcranJeux.Perso.WIDTH, EcranJeux.Perso.HEIGHT);
    }

    private void maJjeu() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        etatTemporel += deltaTime;    //synchro
        personnage.etatTemporel += deltaTime;
        if (Gdx.input.justTouched() || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) { //Si on touche l'??cran, alors...
            Rectangle touche = new Rectangle(Gdx.input.getX() / (Gdx.graphics.getWidth() / camera.viewportWidth) - 1F, (Gdx.graphics.getHeight() - Gdx.input.getY()) / (Gdx.graphics.getHeight() / camera.viewportHeight) - 1F, 2, 2);
            if (etatDuJeu == EtatDuJeu.Lancement) {    //c'est partit!
                etatDuJeu = EtatDuJeu.Marchant;
                personnage.etat = EcranJeux.Perso.Etat.Marche;
            } else if (etatDuJeu == EtatDuJeu.Finissant) {    //c'est finit :(
                Rectangle posVersMenu = new Rectangle(camera.viewportWidth / 2F + echelle, camera.viewportHeight / 2F - 3 * echelle, 2F * echelle, 2F * echelle);
                Rectangle posRejouer = new Rectangle(camera.viewportWidth / 2F - 3 * echelle, camera.viewportHeight / 2F - 3 * echelle, 2F * echelle, 2F * echelle);

                if (touche.overlaps(posVersMenu)) {
                    jeu.setScreen(new EcranMenu(jeu));
                }
                if (touche.overlaps(posRejouer)) {
                    etatDuJeu = EtatDuJeu.Lancement;
                    leGrandReset();    //remise ?? 0 du "monde"
                }
            } else if (etatDuJeu == EtatDuJeu.Marchant){
                Rectangle posPause = new Rectangle( camera.viewportWidth - 2 * echelle,camera.viewportHeight- 2F*echelle,echelle,echelle);
                if (touche.overlaps(posPause)) {
                    etatDuJeu = EtatDuJeu.Pause;
                }
                else if(personnage.aTerre) {
                    personnage.etat = EcranJeux.Perso.Etat.Vole;
                    personnage.velocite.y += VAL_IMPULSION_SAUT;    //le perso saute
                    personnage.aTerre = false;
                }
            } else if (etatDuJeu == EtatDuJeu.Pause){
                Rectangle posVersMenu = new Rectangle(camera.viewportWidth / 2F + 2*echelle, camera.viewportHeight / 2F - 3 * echelle, 2F * echelle, 2F * echelle);
                Rectangle posRejouer = new Rectangle(camera.viewportWidth / 2F - 4 * echelle, camera.viewportHeight / 2F - 3 * echelle, 2F * echelle, 2F * echelle);
                Rectangle posReprendre = new Rectangle(camera.viewportWidth / 2F - echelle, camera.viewportHeight / 2F - 3 * echelle, 2F * echelle, 2F * echelle);

                if (touche.overlaps(posVersMenu)) {
                    jeu.setScreen(new EcranMenu(jeu));
                }else if (touche.overlaps(posRejouer)) {
                    etatDuJeu = EtatDuJeu.Lancement;
                    leGrandReset();    //remise ?? 0 du "monde"
                }else if (touche.overlaps(posReprendre)){
                    etatDuJeu = EtatDuJeu.Marchant;
                }
            }
        }
        if (etatDuJeu != EtatDuJeu.Pause) {
            if (etatDuJeu != EtatDuJeu.Lancement)
                personnage.velocite.add(vecteurGravite); //impl??mente la gravit??

            if (personnage.aTerre) {
                personnage.etat = EcranJeux.Perso.Etat.Marche;
            }
            personnage.velocite.x = MathUtils.clamp(personnage.velocite.x, 0, VITESSE_MAX_X);
            personnage.velocite.y = MathUtils.clamp(personnage.velocite.y, -VITESSE_MAX_Y, VITESSE_MAX_Y);

            float vel = collision(deltaTime);
            if (personnage.position.x < camera.position.x && vel > 0) {
                vel += BONUS_X;
            }
            personnage.position.mulAdd(new Vector2(vel, personnage.velocite.y), deltaTime);    //perso avance

            if (personnage.position.y + EcranJeux.Perso.WIDTH / 2 < 0 || personnage.position.x + EcranJeux.Perso.WIDTH < (camera.position.x - camera.viewportWidth / 2F)) { //il est tomb?? ou il a disparu :O
                etatDuJeu = EtatDuJeu.Finissant;
                personnage.velocite.x = 0;
                personnage.velocite.y = -20;
            }
            // ++mouvements de cam??ra++
            // -personnage trop bas?
            // -personnage trop haut?
            // -suit le personnage.
            if (Math.abs(personnage.position.y + EcranJeux.Perso.HEIGHT) < camera.position.y && personnage.velocite.y > 0) {
                camera.position.mulAdd(new Vector3(personnage.velocite.x, 0, 0F), deltaTime);
            } else if (Math.abs(personnage.position.y + EcranJeux.Perso.HEIGHT) > camera.position.y && personnage.velocite.y < 0) {
                camera.position.mulAdd(new Vector3(personnage.velocite.x, 0, 0F), deltaTime);
            } else {
                camera.position.mulAdd(new Vector3(personnage.velocite.x, personnage.velocite.y, 0F), deltaTime);
            }

            //ne pas sortir de la carte
            camera.position.x = MathUtils.clamp(camera.position.x, camera.viewportWidth / 2F, MONDE_LARGEUR * echelle - camera.viewportWidth / 2F);
            camera.position.y = MathUtils.clamp(camera.position.y, camera.viewportHeight / 2F, MONDE_HAUTEUR * echelle - camera.viewportHeight / 2F);
        }
    }

    public float collision(float deltaTime){
        Array<Rectangle> tuiles = new Array<>();
        float velocite = personnage.velocite.x;
        Vector2 position = new Vector2(personnage.position.x, personnage.position.y);
        position.mulAdd(personnage.velocite,deltaTime);
        Rectangle persoRect = piscineRectangle.obtain();
        position.y-= 0;
        EcranJeux.Perso.WIDTH -= 2;
        persoRect.set((position.x+1)/echelle,position.y/echelle,(EcranJeux.Perso.WIDTH - 2)/echelle, EcranJeux.Perso.HEIGHT/echelle);
        TiledMapTileLayer obstacles = (TiledMapTileLayer) carte.getLayers().get("obstacles");
        TiledMapTileLayer sol = (TiledMapTileLayer) carte.getLayers().get("sol");
        int startX;
        int startY;
        int endX;
        int endY;
        //verification haut/bas
        if (personnage.velocite.y > 0){	//perso qui monte
            startY = (int) (position.y + EcranJeux.Perso.HEIGHT)/echelle;
            endY = (int) (position.y + EcranJeux.Perso.HEIGHT)/echelle;
        }else{	//perso qui descend
            startY = (int) (position.y/echelle);
            endY = (int) (position.y/echelle);
        }
        startX = (int) position.x/echelle;
        endX = (int) ((position.x+ EcranJeux.Perso.WIDTH)/echelle);
         tuiles = obtientTuiles(startX,startY,endX,endY,tuiles, new String[]{"obstacles","sol"});
        for (Rectangle tuile : tuiles){
            if (persoRect.overlaps(tuile)){
                if(obstacles.getCell((int)tuile.x,(int)tuile.y)!= null) {
                    mort();
                }else if(sol.getCell((int)tuile.x,(int)tuile.y)!= null){
                    if (personnage.velocite.y < 0){
                        personnage.aTerre = true;
                    }
                    personnage.velocite.y = 0;
                }
                break;
            }
        }

        //verification droite
        position = new Vector2(personnage.position.x, personnage.position.y);
        position.y += 0;
        EcranJeux.Perso.WIDTH += 2;
        position.x+= 5;
        persoRect.set(position.x/echelle ,position.y/echelle, (EcranJeux.Perso.WIDTH)/echelle +2, EcranJeux.Perso.HEIGHT/echelle);
        if (velocite > 0){	//droite
            startX = (int) (position.x + EcranJeux.Perso.WIDTH)/echelle;
            endX = (int) (position.x + EcranJeux.Perso.WIDTH)/echelle;
        }
        startY = (int) (position.y/echelle);
        endY = (int) ((position.y + EcranJeux.Perso.HEIGHT)/echelle);
        tuiles = obtientTuiles(startX,startY,endX,endY,tuiles,new String[]{"obstacles","sol"});
        for (Rectangle tuile : tuiles){
            if (persoRect.overlaps(tuile)){
                if(obstacles.getCell((int)tuile.x,(int)tuile.y)!= null) {
                    mort();
                }else if(sol.getCell((int)tuile.x,(int)tuile.y)!= null) {
                    velocite = 0;
                }
                break;
            }
        }
        piscineRectangle.free(persoRect);
        return velocite;
    }

    public Array<Rectangle> obtientTuiles(int startX, int startY, int endX, int endY, Array<Rectangle> tiles, String[] calques) {
        piscineRectangle.freeAll(tiles);
        tiles.clear();
        for (String calque : calques) {
            TiledMapTileLayer layer = (TiledMapTileLayer) carte.getLayers().get(calque);
            for (int y = startY; y <= endY; y++) {
                for (int x = startX; x <= endX; x++) {
                    TiledMapTileLayer.Cell cellule = layer.getCell(x, y);
                    if (cellule != null) {
                        Rectangle rect = piscineRectangle.obtain();
                        rect.set(x, y, 1, 1);
                        tiles.add(rect);
                    }
                }
            }
        }
        return tiles;
    }

    @java.lang.Override
    public void resize(int width, int height) {
        //R??S
    }

    @java.lang.Override
    public void pause() {
        //R??S
    }

    @java.lang.Override
    public void resume() {
        //R??S
    }

    @java.lang.Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @java.lang.Override
    public void dispose() {
        jeu.batch.dispose();
        carte.dispose();
        rendu.dispose();
        laMusiqueTropB1.dispose();
    }

    private void leGrandReset() {
        personnage = new EcranJeux.Perso();
        personnage.position.set(PERSO_DEBUT_X, PERSO_DEBUT_Y);
        personnage.velocite.set(0, 0);
        personnage.etat = EcranJeux.Perso.Etat.Marche;
        vecteurGravite.set(LATERAL_GRAVITE, GRAVITE);
        creerCarte((TiledMapTileLayer) layers.get("sol"),(TiledMapTileLayer)layers.get("obstacles"), (int) (camera.viewportWidth/echelle));
        camera.position.set(camera.viewportWidth / 2F, camera.viewportHeight / 2F, 0);
        //laMusiqueTropB1 = Gdx.audio.newMusic(Gdx.files.internal("musique_x.mp3"));
        //laMusiqueTropB1.setLooping(true);
        //laMusiqueTropB1.play();
    }

    public void creerCarte(TiledMapTileLayer sol, TiledMapTileLayer obstacles, int longueurE){
        for (int x = 0; x<sol.getWidth();x++){
            for (int y = 0; y< sol.getHeight();y++){
                sol.setCell(x,y,null);
                obstacles.setCell(x,y,null);
            }
        }
        //sol de d??part
        TextureRegion tSol = new TextureRegion(new Texture("obstacle jeux IPC.PNG"),echelle,echelle);
        TextureRegion tObstacles = new TextureRegion(new Texture("pics jeu png.png"),echelle,52);
        TiledMapTileLayer.Cell cellule = new TiledMapTileLayer.Cell();
        cellule.setTile(new StaticTiledMapTile(tSol));
        TiledMapTileLayer.Cell cellule2 = new TiledMapTileLayer.Cell();
        cellule2.setTile(new StaticTiledMapTile(tObstacles));
        for(int x = 0; x<camera.viewportWidth/echelle ;x++){
            sol.setCell(x,0,cellule);
        }
        int[][] carte = new int[(int)MONDE_LARGEUR-longueurE][(int)MONDE_HAUTEUR];
        int solution = 1;
        int x1 = 2;
        for(int x=0; x< carte.length -1; x+=2){
            //for (int y = solution+3; y< Math.min(solution+6, carte[0].length);y++){carte[x][y] = MathUtils.random(4);}
            int ancSol = solution;
            solution = MathUtils.random(Math.max(solution-2,1),Math.min(solution+x1,(int)MONDE_HAUTEUR-3));
            if (ancSol>solution){
                carte[x][solution] = MathUtils.random(-10,0);
                x1 = Math.min(x1+1,2);
            }else if (ancSol==solution){
                x1 = Math.min(x1+1,2);
                carte[x][solution] = MathUtils.random(-20,0);
            }else{
                x1 -= 1;
                carte[x][solution] = 0;
            }
            carte[x][solution+2] = 0;
            carte[x][solution+1] = 0;
            carte[x][solution -1] = 1;
            carte[x + 1][solution+2] = 0;
            carte[x + 1][solution+1] = 0;
            carte[x + 1][solution] = 0;
            carte[x + 1][solution -1] = 1;
            for (int y = 0; y< carte[0].length;y++){
                if (carte[x][y] == 1){
                    sol.setCell(x+longueurE,y,cellule);
                    sol.setCell(x+1+longueurE,y,cellule);
                }else if (carte[x][y] ==-1 || carte[x][y]==-2){
                    obstacles.setCell(x+longueurE,y-1,cellule2);
                }
                else if(carte[x][y] ==-3 || carte[x][y] == -4){
                    obstacles.setCell(x+longueurE+1,y-1,cellule2);
                }else if(carte[x][y] ==-5){
                    obstacles.setCell(x+longueurE,y-1,cellule2);
                    obstacles.setCell(x+longueurE+1,y-1,cellule2);
                }
            }
        }

    }
    public void mort(){
        etatDuJeu = EtatDuJeu.Finissant;
        personnage.velocite.x = 0;
        personnage.velocite.y = -20;
        if (prefs.getInteger("score",0)<(int)((personnage.position.x-PERSO_DEBUT_X)/echelle)){
            prefs.putInteger("score",(int)((personnage.position.x-PERSO_DEBUT_X)/echelle));
            prefs.flush();
        }
    }

    public enum EtatDuJeu{
        Lancement,Marchant,Finissant,Pause
    }
}
