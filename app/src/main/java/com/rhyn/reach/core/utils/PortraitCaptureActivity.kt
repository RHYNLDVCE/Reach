package com.rhyn.reach.core.utils

import com.journeyapps.barcodescanner.CaptureActivity

/**
 * A custom empty activity that inherits from ZXing's CaptureActivity.
 * We need this so we can explicitly lock the orientation to portrait
 * in the AndroidManifest.xml.
 */
class PortraitCaptureActivity : CaptureActivity()