/*
 * Copyright 2017 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.destinationsol.game;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import org.destinationsol.CommonDrawer;
import org.destinationsol.Const;
import org.destinationsol.GameOptions;
import org.destinationsol.SolApplication;
import org.destinationsol.TextureManager;
import org.destinationsol.common.DebugCol;
import org.destinationsol.common.SolMath;
import org.destinationsol.files.HullConfigManager;
import org.destinationsol.game.asteroid.AsteroidBuilder;
import org.destinationsol.game.chunk.ChunkManager;
import org.destinationsol.game.dra.DraDebugger;
import org.destinationsol.game.dra.DraMan;
import org.destinationsol.game.farBg.FarBackgroundManagerOld;
import org.destinationsol.game.input.AiPilot;
import org.destinationsol.game.input.BeaconDestProvider;
import org.destinationsol.game.input.Pilot;
import org.destinationsol.game.input.UiControlledPilot;
import org.destinationsol.game.item.Gun;
import org.destinationsol.game.item.ItemContainer;
import org.destinationsol.game.item.ItemManager;
import org.destinationsol.game.item.LootBuilder;
import org.destinationsol.game.item.SolItem;
import org.destinationsol.game.particle.EffectTypes;
import org.destinationsol.game.particle.PartMan;
import org.destinationsol.game.particle.SpecialEffects;
import org.destinationsol.game.planet.Planet;
import org.destinationsol.game.planet.PlanetManager;
import org.destinationsol.game.planet.SolSystem;
import org.destinationsol.game.planet.SunSingleton;
import org.destinationsol.game.screens.GameScreens;
import org.destinationsol.game.ship.FarShip;
import org.destinationsol.game.ship.ShipAbility;
import org.destinationsol.game.ship.ShipBuilder;
import org.destinationsol.game.ship.SloMo;
import org.destinationsol.game.ship.SolShip;
import org.destinationsol.game.ship.hulls.HullConfig;
import org.destinationsol.game.sound.OggSoundManager;
import org.destinationsol.game.sound.SpecialSounds;
import org.destinationsol.ui.DebugCollector;
import org.destinationsol.ui.TutorialManager;
import org.destinationsol.ui.UiDrawer;

import java.util.ArrayList;
import java.util.List;

public class SolGame {
    private final GameScreens gameScreens;
    private final SolCam camera;
    private final ObjectManager objectManager;
    private final SolApplication solApplication;
    private final DraMan draMan;
    private final PlanetManager planetManager;
    private final TextureManager textureManager;
    private final ChunkManager chunkManager;
    private final PartMan partMan;
    private final AsteroidBuilder asteroidBuilder;
    private final LootBuilder lootBuilder;
    private final ShipBuilder shipBuilder;
    private final HullConfigManager hullConfigManager;
    private final GridDrawer gridDrawer;
    private final FarBackgroundManagerOld farBackgroundManagerOld;
    private final FactionManager factionManager;
    private final MapDrawer mapDrawer;
    private final ShardBuilder shardBuilder;
    private final ItemManager itemManager;
    private final StarPort.Builder starPortBuilder;
    private final OggSoundManager soundManager;
    private final DraDebugger draDebugger;
    private final SpecialSounds specialSounds;
    private final SpecialEffects specialEffects;
    private final GameColors gameColors;
    private final BeaconHandler beaconHandler;
    private final MountDetectDrawer mountDetectDrawer;
    private final TutorialManager tutorialManager;
    private final GalaxyFiller galaxyFiller;
    private final ArrayList<SolItem> respawnItems;
    private SolShip myHero;
    private float myTimeStep;
    private float myTime;
    private boolean myPaused;
    private StarPort.Transcendent myTranscendentHero;
    private float myTimeFactor;
    private float myRespawnMoney;
    private HullConfig myRespawnHull;

    public SolGame(SolApplication cmp, String shipName, TextureManager textureManager, boolean tut, CommonDrawer commonDrawer) {
        solApplication = cmp;
        GameDrawer drawer = new GameDrawer(textureManager, commonDrawer);
        gameColors = new GameColors();
        soundManager = solApplication.getSoundManager();
        specialSounds = new SpecialSounds(soundManager);
        draMan = new DraMan(drawer);
        camera = new SolCam(drawer.r);
        gameScreens = new GameScreens(drawer.r, cmp);
        tutorialManager = tut ? new TutorialManager(commonDrawer.r, gameScreens, cmp.isMobile(), cmp.getOptions(), this) : null;
        this.textureManager = textureManager;
        farBackgroundManagerOld = new FarBackgroundManagerOld(this.textureManager);
        shipBuilder = new ShipBuilder();
        EffectTypes effectTypes = new EffectTypes();
        specialEffects = new SpecialEffects(effectTypes, this.textureManager, gameColors);
        itemManager = new ItemManager(this.textureManager, soundManager, effectTypes, gameColors);
        AbilityCommonConfigs abilityCommonConfigs = new AbilityCommonConfigs(effectTypes, this.textureManager, gameColors, soundManager);
        hullConfigManager = new HullConfigManager(itemManager, abilityCommonConfigs);
        SolNames solNames = new SolNames();
        planetManager = new PlanetManager(this.textureManager, hullConfigManager, gameColors, itemManager);
        SolContactListener contactListener = new SolContactListener(this);
        factionManager = new FactionManager();
        objectManager = new ObjectManager(contactListener, factionManager);
        gridDrawer = new GridDrawer(textureManager);
        chunkManager = new ChunkManager(this.textureManager);
        partMan = new PartMan();
        asteroidBuilder = new AsteroidBuilder(this.textureManager);
        lootBuilder = new LootBuilder();
        mapDrawer = new MapDrawer(this.textureManager, commonDrawer.h);
        shardBuilder = new ShardBuilder(this.textureManager);
        galaxyFiller = new GalaxyFiller();
        starPortBuilder = new StarPort.Builder();
        draDebugger = new DraDebugger();
        beaconHandler = new BeaconHandler(textureManager);
        mountDetectDrawer = new MountDetectDrawer(textureManager);
        respawnItems = new ArrayList<>();
        myTimeFactor = 1;

        // from this point we're ready!
        planetManager.fill(solNames);
        galaxyFiller.fill(this, hullConfigManager, itemManager);
        createPlayer(shipName);
        SolMath.checkVectorsTaken(null);
    }

    // uh, this needs refactoring
    private void createPlayer(String shipName) {
        Vector2 pos = galaxyFiller.getPlayerSpawnPos(this);
        camera.setPos(pos);

        Pilot pilot;
        if (solApplication.getOptions().controlType == GameOptions.CONTROL_MOUSE) {
            beaconHandler.init(this, pos);
            pilot = new AiPilot(new BeaconDestProvider(), true, Faction.LAANI, false, "you", Const.AI_DET_DIST);
        } else {
            pilot = new UiControlledPilot(gameScreens.mainScreen);
        }

        ShipConfig shipConfig = shipName == null ? SaveManager.readShip(hullConfigManager, itemManager) : ShipConfig.load(hullConfigManager, shipName, itemManager);

        float money = myRespawnMoney != 0 ? myRespawnMoney : tutorialManager != null ? 200 : shipConfig.money;

        HullConfig hull = myRespawnHull != null ? myRespawnHull : shipConfig.hull;

        String itemsStr = !respawnItems.isEmpty() ? "" : shipConfig.items;

        boolean giveAmmo = shipName != null && respawnItems.isEmpty();
        myHero = shipBuilder.buildNewFar(this, new Vector2(pos), null, 0, 0, pilot, itemsStr, hull, null, true, money, null, giveAmmo).toObj(this);

        ItemContainer ic = myHero.getItemContainer();
        if (!respawnItems.isEmpty()) {
            for (int i1 = 0, sz = respawnItems.size(); i1 < sz; i1++) {
                SolItem item = respawnItems.get(i1);
                ic.add(item);
                // Ensure that previously equipped items stay equipped
                if (item.isEquipped() > 0) {
                    if (item instanceof Gun) {
                        myHero.maybeEquip(this, item, item.isEquipped() == 2, true);
                    } else {
                        myHero.maybeEquip(this, item, true);
                    }
                }
            }
        } else if (DebugOptions.GOD_MODE) {
            itemManager.addAllGuns(ic);
        } else if (tutorialManager != null) {
            for (int i = 0; i < 50; i++) {
                if (ic.groupCount() > 1.5f * Const.ITEM_GROUPS_PER_PAGE) {
                    break;
                }
                SolItem it = itemManager.random();
                if (!(it instanceof Gun) && it.getIcon(this) != null && ic.canAdd(it)) {
                    ic.add(it.copy());
                }
            }
        }
        ic.seenAll();

        // Don't change equipped items across load/respawn
        //AiPilot.reEquip(this, myHero);

        objectManager.addObjDelayed(myHero);
        objectManager.resetDelays();
    }

    public void onGameEnd() {
        saveShip();
        objectManager.dispose();
    }

    public void saveShip() {
        if (tutorialManager != null) {
            return;
        }
        HullConfig hull;
        float money;
        ArrayList<SolItem> items;
        if (myHero != null) {
            hull = myHero.getHull().config;
            money = myHero.getMoney();
            items = new ArrayList<SolItem>();
            for (List<SolItem> group : myHero.getItemContainer()) {
                for (SolItem i : group) {
                    items.add(0, i);
                }
            }
        } else if (myTranscendentHero != null) {
            FarShip farH = myTranscendentHero.getShip();
            hull = farH.getHullConfig();
            money = farH.getMoney();
            items = new ArrayList<SolItem>();
            for (List<SolItem> group : farH.getIc()) {
                for (SolItem i : group) {
                    items.add(0, i);
                }
            }
        } else {
            hull = myRespawnHull;
            money = myRespawnMoney;
            items = respawnItems;
        }
        SaveManager.writeShip(hull, money, items, this);
    }

    public GameScreens getScreens() {
        return gameScreens;
    }

    public void update() {
        draDebugger.update(this);

        if (myPaused) {
            camera.updateMap(this); // update zoom only for map
            mapDrawer.update(this); // animate map icons
            return;
        }

        myTimeFactor = DebugOptions.GAME_SPEED_MULTIPLIER;
        if (myHero != null) {
            ShipAbility ability = myHero.getAbility();
            if (ability instanceof SloMo) {
                float factor = ((SloMo) ability).getFactor();
                myTimeFactor *= factor;
            }
        }
        myTimeStep = Const.REAL_TIME_STEP * myTimeFactor;
        myTime += myTimeStep;

        planetManager.update(this);
        camera.update(this);
        chunkManager.update(this);
        mountDetectDrawer.update(this);
        objectManager.update(this);
        draMan.update(this);
        mapDrawer.update(this);
        soundManager.update(this);
        beaconHandler.update(this);

        myHero = null;
        myTranscendentHero = null;
        List<SolObject> objs = objectManager.getObjs();
        for (int i = 0, objsSize = objs.size(); i < objsSize; i++) {
            SolObject obj = objs.get(i);
            if ((obj instanceof SolShip)) {
                SolShip ship = (SolShip) obj;
                Pilot prov = ship.getPilot();
                if (prov.isPlayer()) {
                    myHero = ship;
                    break;
                }
            }
            if (obj instanceof StarPort.Transcendent) {
                StarPort.Transcendent trans = (StarPort.Transcendent) obj;
                FarShip ship = trans.getShip();
                if (ship.getPilot().isPlayer()) {
                    myTranscendentHero = trans;
                    break;
                }
            }
        }

        if (tutorialManager != null) {
            tutorialManager.update();
        }
    }

    public void draw() {
        draMan.draw(this);
    }

    public void drawDebug(GameDrawer drawer) {
        if (DebugOptions.GRID_SZ > 0) {
            gridDrawer.draw(drawer, this, DebugOptions.GRID_SZ, drawer.debugWhiteTex);
        }
        planetManager.drawDebug(drawer, this);
        objectManager.drawDebug(drawer, this);
        if (DebugOptions.ZOOM_OVERRIDE != 0) {
            camera.drawDebug(drawer);
        }
        drawDebugPoint(drawer, DebugOptions.DEBUG_POINT, DebugCol.POINT);
        drawDebugPoint(drawer, DebugOptions.DEBUG_POINT2, DebugCol.POINT2);
        drawDebugPoint(drawer, DebugOptions.DEBUG_POINT3, DebugCol.POINT3);
    }

    private void drawDebugPoint(GameDrawer drawer, Vector2 dp, Color col) {
        if (dp.x != 0 || dp.y != 0) {
            float sz = camera.getRealLineWidth() * 5;
            drawer.draw(drawer.debugWhiteTex, sz, sz, sz / 2, sz / 2, dp.x, dp.y, 0, col);
        }
    }

    public float getTimeStep() {
        return myTimeStep;
    }

    public SolCam getCam() {
        return camera;
    }

    public SolApplication getCmp() {
        return solApplication;
    }

    public DraMan getDraMan() {
        return draMan;
    }

    public ObjectManager getObjMan() {
        return objectManager;
    }

    public TextureManager getTexMan() {
        return textureManager;
    }

    public PlanetManager getPlanetMan() {
        return planetManager;
    }

    public PartMan getPartMan() {
        return partMan;
    }

    public AsteroidBuilder getAsteroidBuilder() {
        return asteroidBuilder;
    }

    public LootBuilder getLootBuilder() {
        return lootBuilder;
    }

    public SolShip getHero() {
        return myHero;
    }

    public ShipBuilder getShipBuilder() {
        return shipBuilder;
    }

    public ItemManager getItemMan() {
        return itemManager;
    }

    public HullConfigManager getHullConfigs() {
        return hullConfigManager;
    }

    public boolean isPaused() {
        return myPaused;
    }

    public void setPaused(boolean paused) {
        myPaused = paused;
        DebugCollector.warn(myPaused ? "game paused" : "game resumed");
    }

    public void respawn() {
        if (myHero != null) {
            beforeHeroDeath();
            objectManager.removeObjDelayed(myHero);
        } else if (myTranscendentHero != null) {
            FarShip farH = myTranscendentHero.getShip();
            setRespawnState(farH.getMoney(), farH.getIc(), farH.getHullConfig());
            objectManager.removeObjDelayed(myTranscendentHero);
        }
        createPlayer(null);
    }

    public FactionManager getFactionMan() {
        return factionManager;
    }

    public boolean isPlaceEmpty(Vector2 pos, boolean considerPlanets) {
        Planet np = planetManager.getNearestPlanet(pos);
        if (considerPlanets) {
            boolean inPlanet = np.getPos().dst(pos) < np.getFullHeight();
            if (inPlanet) {
                return false;
            }
        }
        SolSystem ns = planetManager.getNearestSystem(pos);
        if (ns.getPos().dst(pos) < SunSingleton.SUN_HOT_RAD) {
            return false;
        }
        List<SolObject> objs = objectManager.getObjs();
        for (int i = 0, objsSize = objs.size(); i < objsSize; i++) {
            SolObject o = objs.get(i);
            if (!o.hasBody()) {
                continue;
            }
            if (pos.dst(o.getPosition()) < objectManager.getRadius(o)) {
                return false;
            }
        }
        List<FarObjData> farObjs = objectManager.getFarObjs();
        for (int i = 0, farObjsSize = farObjs.size(); i < farObjsSize; i++) {
            FarObjData fod = farObjs.get(i);
            FarObj o = fod.fo;
            if (!o.hasBody()) {
                continue;
            }
            if (pos.dst(o.getPos()) < o.getRadius()) {
                return false;
            }
        }
        return true;
    }

    public MapDrawer getMapDrawer() {
        return mapDrawer;
    }

    public ShardBuilder getShardBuilder() {
        return shardBuilder;
    }

    public FarBackgroundManagerOld getFarBgManOld() {
        return farBackgroundManagerOld;
    }

    public GalaxyFiller getGalaxyFiller() {
        return galaxyFiller;
    }

    public StarPort.Builder getStarPortBuilder() {
        return starPortBuilder;
    }

    public StarPort.Transcendent getTranscendentHero() {
        return myTranscendentHero;
    }

    public GridDrawer getGridDrawer() {
        return gridDrawer;
    }

    public OggSoundManager getSoundManager() {
        return soundManager;
    }

    public float getTime() {
        return myTime;
    }

    public void drawDebugUi(UiDrawer uiDrawer) {
        draDebugger.draw(uiDrawer);
    }

    public SpecialSounds getSpecialSounds() {
        return specialSounds;
    }

    public SpecialEffects getSpecialEffects() {
        return specialEffects;
    }

    public GameColors getCols() {
        return gameColors;
    }

    public float getTimeFactor() {
        return myTimeFactor;
    }

    public BeaconHandler getBeaconHandler() {
        return beaconHandler;
    }

    public MountDetectDrawer getMountDetectDrawer() {
        return mountDetectDrawer;
    }

    public TutorialManager getTutMan() {
        return tutorialManager;
    }

    public void beforeHeroDeath() {
        if (myHero == null) {
            return;
        }

        float money = myHero.getMoney();
        ItemContainer ic = myHero.getItemContainer();

        setRespawnState(money, ic, myHero.getHull().config);

        myHero.setMoney(money - myRespawnMoney);
        for (SolItem item : respawnItems) {
            ic.remove(item);
        }
    }

    private void setRespawnState(float money, ItemContainer ic, HullConfig hullConfig) {
        myRespawnMoney = .75f * money;
        myRespawnHull = hullConfig;
        respawnItems.clear();
        for (List<SolItem> group : ic) {
            for (SolItem item : group) {
                boolean equipped = myHero == null || myHero.maybeUnequip(this, item, false);
                if (equipped || SolMath.test(.75f)) {
                    respawnItems.add(0, item);
                }
            }
        }
    }
}
