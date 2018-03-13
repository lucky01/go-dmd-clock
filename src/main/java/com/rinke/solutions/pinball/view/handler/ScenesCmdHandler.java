package com.rinke.solutions.pinball.view.handler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.lang3.StringUtils;

import com.rinke.solutions.beans.Autowired;
import com.rinke.solutions.beans.Bean;
import com.rinke.solutions.pinball.PinDmdEditor;
import com.rinke.solutions.pinball.animation.Animation;
import com.rinke.solutions.pinball.animation.Animation.EditMode;
import com.rinke.solutions.pinball.animation.CompiledAnimation;
import com.rinke.solutions.pinball.model.Bookmark;
import com.rinke.solutions.pinball.model.PalMapping;
import com.rinke.solutions.pinball.util.MessageUtil;
import com.rinke.solutions.pinball.view.model.ViewModel;

@Bean
@Slf4j
public class ScenesCmdHandler extends AbstractListCmdHandler implements ViewBindingHandler {

	@Autowired private MessageUtil messageUtil;
	private EditMode mutable[] = { EditMode.REPLACE, EditMode.COLMASK, EditMode.FOLLOW, EditMode.LAYEREDCOL };
	@Autowired private RecordingsCmdHandler recordingsCmdHandler;
	@Autowired private DrawCmdHandler drawCmdHandler;

	public ScenesCmdHandler(ViewModel vm) {
		super(vm);
	}
	
	public void onSelectedSceneChanged(CompiledAnimation o, CompiledAnimation nextScene) {
		log.info("onSceneSelectionChanged: {}", nextScene);
		Animation current = o;
		// detect changes
		if( current == null && nextScene == null ) return;
		if( nextScene != null && current != null && nextScene.getDesc().equals(current.getDesc())) return;
		
		if( current != null ) {
			vm.scenesPosMap.put(current.getDesc(), current.actFrame);
			current.commitDMDchanges(vm.dmd,vm.hashes.get(vm.selectedHashIndex));
			vm.setDirty(current.isDirty());
		}
		if( nextScene != null ) {
			// deselect recording
			vm.cutInfo.reset();
			vm.setSelection(null);
			vm.setSelectedRecording(null);
			
		//	v.goDmdGroup.updateAnimation(nextScene);

			vm.setMaskEnabled(nextScene.getEditMode().useMask);
			vm.setMaskSpinnerEnabled(false);
			// just to enasure a reasonable default
			if( nextScene.getEditMode() == null || nextScene.getEditMode().equals(EditMode.FIXED) ) {
				// old animation may be saved with wrong edit mode
				nextScene.setEditMode(EditMode.REPLACE);
			}
			vm.availableEditModes.clear();
			vm.availableEditModes.addAll(Arrays.asList(mutable));
			
			recordingsCmdHandler.setEnableHashButtons(nextScene.getEditMode().useMask);
			
			vm.setSelectedScene(nextScene);

			int numberOfPlanes = nextScene.getRenderer().getNumberOfPlanes();
			if( numberOfPlanes == 5) {
				numberOfPlanes = 4;
			}
			if (numberOfPlanes == 3) {
				numberOfPlanes = 2;
				// v.goDmdGroup.transitionCombo.select(1);
			} else {
				// v.goDmdGroup.transitionCombo.select(0);
			}

			vm.setSelectedPaletteByIndex(nextScene.getPalIndex());
			
			vm.setSelectedEditMode(nextScene.getEditMode());

			drawCmdHandler.setDrawMaskByEditMode(nextScene.getEditMode());// doesnt fire event?????
			
			vm.dmd.setNumberOfSubframes(numberOfPlanes);
			
			vm.setPaletteToolPlanes(vm.useGlobalMask?1:numberOfPlanes);

			recordingsCmdHandler.setPlayingAni(nextScene, vm.scenesPosMap.getOrDefault(nextScene.getDesc(), 0));
			
		} else {
			vm.setSelectedScene(null);
		}
		// v.goDmdGroup.updateAniModel(nextScene);
		vm.setDeleteSceneEnabled(nextScene!=null);
		vm.setBtnSetScenePalEnabled(nextScene!=null);
	}
	
	public void onDeleteScene() {
		Animation a = vm.selectedScene;
		ArrayList<String> res = new ArrayList<>();
		if( a!=null) {
			for( PalMapping pm : vm.keyframes.values()) {
				if( a.getDesc().equals(pm.frameSeqName) ) {
					res.add( a.getDesc() );
				}
			}
		}
		if( res.isEmpty() ) {
			onRemove(a, vm.scenes);
		} else {
			messageUtil.warn("Scene cannot be deleted", "It is used by "+res);
		}
	}

	public void onSortScenes() {
		onSortAnimations(vm.scenes);
	}
	
	// called when scene gets renamed
	private void updateBookmarkNames(String old, String newName) {
		for( Set<Bookmark> bookmarks : vm.bookmarksMap.values()) {
			Iterator<Bookmark> i = bookmarks.iterator();
			while(i.hasNext() ) {
				Bookmark bm = i.next();
				if( bm.name.equals(old) ) {
					i.remove();
					bookmarks.add( new Bookmark(newName, bm.pos));
					break;
				}
			}
		}
		
	}

	/**
	 * if a scene gets renamed, this update function is called.
	 * if newKey is not equal to old, the refering pal mappings gets updated
	 * @param oldKey old name of the scene
	 * @param newKey new name of scene
	 */
	private void updatePalMappingsSceneNames(String oldKey, String newKey) {
		if( StringUtils.equals(oldKey, newKey) ) return;
		vm.keyframes.values().forEach(p->{
			if( p.frameSeqName != null && p.frameSeqName.equals(oldKey)) {
				p.frameSeqName = newKey;
			}
		});
	}

	public void onSetScenePalette() {
		if (vm.selectedScene!=null && vm.selectedPalette != null) {
			CompiledAnimation scene = vm.selectedScene;
			scene.setPalIndex(vm.selectedPalette.index);
			log.info("change pal index in scene {} to {}", scene.getDesc(), vm.selectedPalette.index);
			for(PalMapping p : vm.keyframes.values()) {
				if( p.switchMode!=null && p.switchMode.hasSceneReference) {
					if(p.frameSeqName.equals(scene.getDesc())) {
						log.info("adjusting pal index for keyframe {} to {}", p, vm.selectedPalette.index);
						p.palIndex = vm.selectedPalette.index;
					}
				}
			}
		}
	}



	public void onRenameScene(String oldName, String newName){
		updateAnimationMapKey(oldName, newName, vm.scenes);
		updateBookmarkNames( oldName, newName );
		updatePalMappingsSceneNames(oldName, newName);
		vm.setDirty(true);
	}
	
}