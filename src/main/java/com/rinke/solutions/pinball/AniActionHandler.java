package com.rinke.solutions.pinball;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.google.common.collect.Lists;
import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.animation.AniWriter;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.AnimationFactory;
import com.rinke.solutions.pinball.animation.AnimationType;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Palette;
import com.rinke.solutions.pinball.ui.Progress;
import com.rinke.solutions.pinball.util.FileChooserUtil;
import com.rinke.solutions.pinball.view.model.Model;

@Slf4j
@Bean
public class AniActionHandler {
	
	@Autowired
	FileChooserUtil fileChooserUtil;
	private Shell shell;
	private Model model;
	
	public AniActionHandler(Shell shell, Model model) {
		this.shell = shell;
		this.model = model;
	}
	
	protected Progress getProgress() {
		return new Progress(shell);
	}

	public void onSaveAniWithFC(String defaultName, int version) {
		String filename = fileChooserUtil.choose(SWT.SAVE, defaultName, new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(model.recordings.values(), filename, version, true);
		}
	}

	public void storeAnimations(Collection<Animation> anis, String filename, int version, boolean saveAll) {
		java.util.List<Animation> anisToSave = anis.stream().filter(a -> saveAll || a.isDirty()).collect(Collectors.toList());
		if( anisToSave.isEmpty() ) return;// Pair.of(0, Collections.emptyMap());
		Progress progress = getProgress();
		AniWriter aniWriter = new AniWriter(anisToSave, filename, version, model.palettes, progress);
		if( progress != null ) 
			progress.open(aniWriter);
		else
			aniWriter.run();
		anisToSave.forEach(a->a.setDirty(false));
	}

	public void onLoadAniWithFC(boolean append) {
		List<String> filenames = fileChooserUtil.chooseMulti(SWT.OPEN|SWT.MULTI, null, new String[] { "*.properties;*.ani;*.txt.gz;*.pcap;*.pcap.gz;*.*" }, new String[] { "Animationen",
				"properties, txt.gz, ani, mov" });

		for(String filename : filenames) {
			loadAni(filename, append, true);
		}
	}

	boolean extensionIs(String name, String... args) {
		for (String ext : args) {
			if (name.endsWith(ext))
				return true;
		}
		return false;
	}

	public java.util.List<Animation> loadAni(String filename, boolean append, boolean populateModel) {
		java.util.List<Animation> loadedList = new ArrayList<>();
		try {
		if (filename.endsWith(".ani")) {
			loadedList.addAll(CompiledAnimation.read(filename));
		} else if (filename.endsWith(".txt.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.MAME));
		} else if (filename.endsWith(".properties")) {
			loadedList.addAll(AnimationFactory.createAnimationsFromProperties(filename,shell));
		} else if (extensionIs(filename, ".pcap", ".pcap.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PCAP));
		} else if (extensionIs(filename, ".dump", ".dump.gz")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.PINDUMP, shell));
		} else if (extensionIs(filename, ".gif")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.GIF));
		} else if (extensionIs(filename, ".mp4", ".3gp", ".avi")) {
			loadedList.add(Animation.buildAnimationFromFile(filename, AnimationType.VIDEO, shell));
		}
		log.info("loaded {} animations from {}", loadedList.size(), filename);
		} catch( IOException e) {
			log.error("error load anis from {}", filename, e);
		}
		if (populateModel) {
			if (!append)
				model.inputFiles.clear();
			if (!model.inputFiles.contains(filename))
				model.inputFiles.add(filename);
		}

		// animationHandler.setAnimations(sourceAnis);
		if (!append) {
			model.recordings.clear();
			model.scenes.clear();
			//model.playingAnis.clear();
		}
		DMD dmd = new DMD(model.dmdSize.width,model.dmdSize.height);
		for (Animation ani : loadedList) {
			if( ani instanceof CompiledAnimation ) {
				model.inputFiles.remove(filename);
				ani.setProjectAnimation(true);
				populateAni((CompiledAnimation)ani, model.scenes);
			} else {
				populateAni(ani, model.recordings);
			}	
			
			ani.init(dmd);
			populatePalette(ani, model.palettes);
		}
		// TODO not here
		// model.recentAnimationsMenuManager.populateRecent(filename);
		model.dirty = true;
		return loadedList;
	}

	private <T extends Animation> void populateAni( T ani, Map<String, T> anis) {
		if (anis.containsKey(ani.getDesc())) {
			int i = 0;
			String desc = ani.getDesc();
			while (i < 1000) {
				String newDesc = desc + "-" + i;
				if (!anis.containsKey(newDesc)) {
					ani.setDesc(newDesc);
					break;
				}
				i++;
			}
		}
		anis.put(ani.getDesc(), ani);
	}

	private void populatePalette(Animation ani, List<Palette> palettes) {
		if (ani.getAniColors() != null) {
			// if loaded colors with animations propagate as palette
			boolean colorsMatch = false;
			for (Palette p : palettes) {
				if (p.sameColors(ani.getAniColors())) {
					colorsMatch = true;
					ani.setPalIndex(p.index);
					break;
				}
			}
			if (!colorsMatch) {
				Palette aniPalette = new Palette(ani.getAniColors(), palettes.size(), ani.getDesc());
				palettes.add(aniPalette);
				ani.setPalIndex(aniPalette.index);
			}
		}
	}

	public void onSaveSingleAniWithFC(Animation ani, int version) {
		String filename = fileChooserUtil.choose(SWT.SAVE, ani.getDesc(), new String[] { "*.ani" }, new String[] { "Animations" });
		if (filename != null) {
			log.info("store animation to {}", filename);
			storeAnimations(Lists.newArrayList(ani), filename, version, true);
		}
	}

}
