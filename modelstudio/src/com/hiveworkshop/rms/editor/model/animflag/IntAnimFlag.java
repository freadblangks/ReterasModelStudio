package com.hiveworkshop.rms.editor.model.animflag;

import com.hiveworkshop.rms.editor.model.TimelineContainer;
import com.hiveworkshop.rms.parsers.mdlx.InterpolationType;
import com.hiveworkshop.rms.parsers.mdlx.timeline.MdlxUInt32Timeline;

/**
 * A java class for MDL "motion flags," such as Alpha, Translation, Scaling, or
 * Rotation. AnimFlags are not "real" things from an MDL and are given this name
 * by me, as an invented java class to simplify the programming
 *
 * Eric Theller 11/5/2011
 */
public class IntAnimFlag extends AnimFlag<Integer> {

	public IntAnimFlag(String title) {
		super(title);
	}

	public IntAnimFlag(AnimFlag<Integer> af) {
		super(af);
	}

	public IntAnimFlag(final MdlxUInt32Timeline timeline) {
		super(timeline);

		if(timeline.interpolationType != InterpolationType.DONT_INTERP){
			System.out.println("(IntAnimFlag) timeline \"" + name + "\" has interpolation type: " + timeline.interpolationType.name());
		}

		final long[] frames = timeline.frames;
		final Object[] values = timeline.values;
		final Object[] inTans = timeline.inTans;
		final Object[] outTans = timeline.outTans;

		if (frames.length > 0) {
			boolean hasTangents = interpolationType.tangential();

			for (int i = 0, l = frames.length; i < l; i++) {
				final long[] value = (long[]) values[i];
				int valueAsObject = 0;
				Integer inTanAsObject = null;
				Integer outTanAsObject = null;

				valueAsObject = (int) value[0];

				if (hasTangents) {
					inTanAsObject = (int) ((long[]) inTans[i])[0];
					outTanAsObject = (int) ((long[]) outTans[i])[0];
				}

				addEntry((int) frames[i], valueAsObject, inTanAsObject, outTanAsObject);
			}
		}
	}


	public AnimFlag<Integer> getEmptyCopy(){
		IntAnimFlag newFlag = new IntAnimFlag(name);
		newFlag.setSettingsFrom(this);
		return newFlag;
	}
	public AnimFlag<Integer> deepCopy(){
		return new IntAnimFlag(this);
	}

	public Integer cloneValue(Object value) {
		if(value instanceof Integer){
			return (Integer) value;
		}
		return null;
	}

	@Override
	protected Integer getIdentity(int typeId) {
		return (int) identity(typeId);
	}

	@Override
	public Integer getInterpolatedValue(Integer floorTime, Integer ceilTime, float timeFactor) {
		Entry<Integer> entryFloor = entryMap.get(floorTime);
		Entry<Integer> entryCeil = entryMap.get(ceilTime);
		return getInterpolatedValue(entryFloor, entryCeil, 1);
	}

	@Override
	public Integer getInterpolatedValue(Entry<Integer> entryFloor, Entry<Integer> entryCeil, float timeFactor) {
		Integer floorValue = entryFloor.getValue();
		Integer floorOutTan = entryFloor.getOutTan();

		Integer ceilValue = entryCeil.getValue();
		Integer ceilInTan = entryCeil.getInTan();

		switch (typeid) {
//			case TRANSLATION, SCALING, COLOR -> {
			case TEXTUREID -> {
				return switch (interpolationType) {
//					case BEZIER -> int.getBezier(floorValue, floorOutTan, ceilInTan, ceilValue, timeFactor);
//					case DONT_INTERP -> floorValue;
//					case HERMITE -> int.getHermite(floorValue, floorOutTan, ceilInTan, ceilValue, timeFactor);
//					case LINEAR -> int.getLerped(floorValue, ceilValue, timeFactor);
					case DONT_INTERP, BEZIER, HERMITE, LINEAR -> floorValue;
				};
			}
		}
		throw new IllegalStateException();
	}

	@Override
	public void setInterpType(final InterpolationType interpolationType) {
		this.interpolationType = InterpolationType.DONT_INTERP;
	}

	@Override
	public MdlxUInt32Timeline toMdlx(final TimelineContainer container) {
		final MdlxUInt32Timeline mdlxTimeline = new MdlxUInt32Timeline();

		mdlxTimeline.name = FlagUtils.getWar3ID(name, container);
		mdlxTimeline.interpolationType = interpolationType;
		mdlxTimeline.globalSequenceId = getGlobalSeqId();

		long[] tempFrames = new long[entryMap.size()];
		long[][] tempValues = new long[entryMap.size()][];
		long[][] tempInTans = new long[entryMap.size()][];
		long[][] tempOutTans = new long[entryMap.size()][];

		boolean hasTangents = mdlxTimeline.interpolationType.tangential();

		for (int i = 0, l = entryMap.size(); i < l; i++) {
			Integer value = getValueFromIndex(i);

			tempFrames[i] = getTimeFromIndex(i);
			tempValues[i] = (new long[] {value.longValue()});

			if (hasTangents) {
				tempInTans[i] = new long[] {getInTanFromIndex(i).longValue()};
				tempOutTans[i] = new long[] {getOutTanFromIndex(i).longValue()};
			} else {
				tempInTans[i] = new long[] {0};
				tempOutTans[i] = new long[] {0};
			}
		}

		mdlxTimeline.frames = tempFrames;
		mdlxTimeline.values = tempValues;
		mdlxTimeline.inTans = tempInTans;
		mdlxTimeline.outTans = tempOutTans;

		return mdlxTimeline;
	}

	@Override
	public float[] getTbcFactor(float bias, float tension, float continuity) {
		return getTCB(-1, bias, tension, continuity);
	}

	@Override
	public void calcNewTans(float[] factor, Entry<Integer> next, Entry<Integer> prev, Entry<Integer> cur, int animationLength) {
		// Calculating the derivatives in point Cur (for count cells)

		int currPrev = cur.value;
		if (prev != null) {
			currPrev -= prev.value;
		}
		int nextCurr = -cur.value;
		if (next != null) {
			nextCurr += next.value;
		}

		cur.inTan = (int) (currPrev * factor[0] + nextCurr * factor[1]);
		cur.outTan = (int) (currPrev * factor[2] + nextCurr * factor[3]);

		if (next != null && prev != null && !next.time.equals(prev.time)) {
			float timeBetweenFrames = (next.time - prev.time + animationLength) % animationLength;
			int timeToPrevFrame = (cur.time - prev.time + animationLength) % animationLength;
			int timeToNextFrame = (next.time - cur.time + animationLength) % animationLength;

			float inAdj = 2 * timeToPrevFrame / timeBetweenFrames;
			float outAdj = 2 * timeToNextFrame / timeBetweenFrames;
			cur.inTan *= (int) inAdj;
			cur.outTan *= (int) outAdj;
		}
	}
}
