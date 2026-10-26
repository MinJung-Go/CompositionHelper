"""Run with PYTHONPATH=tools python -m unittest discover -s tools -p 'test_preview_llm_color.py'."""
import unittest
import numpy as np
from preview_llm_color import validate, apply_plan, rgb_to_hsl, hsl_to_rgb, CURVE_X

class ColorRendererTests(unittest.TestCase):
    def test_hsl_roundtrip(self):
        pixels=np.random.default_rng(42).random((100,100,3),dtype=np.float32)
        np.testing.assert_allclose(hsl_to_rgb(*rgb_to_hsl(pixels)),pixels,atol=1e-6)

    def test_identity(self):
        pixels=np.random.default_rng(43).random((50,50,3),dtype=np.float32)
        np.testing.assert_allclose(apply_plan(pixels,validate({'file':'test'})),pixels,atol=1e-6)

    def test_neutral_pixels_not_tinted_by_hsl(self):
        pixels=np.repeat(np.linspace(0,1,101,dtype=np.float32)[None,:,None],3,axis=2)
        plan=validate({'file':'test','hsl':[{'band':'red','hue_shift':10,'saturation_percent':25,'lightness_points':7}]})
        np.testing.assert_allclose(apply_plan(pixels,plan),pixels,atol=1e-6)

    def test_numeric_model_band_is_preserved(self):
        plan=validate({'file':'test','hsl':[{'band':225,'saturation_percent':-7}]})
        self.assertEqual(plan['hsl'][0]['band'],'blue')
        self.assertEqual(plan['hsl'][0]['saturation_percent'],-7)

    def test_invalid_band_rejected(self):
        with self.assertRaises(ValueError):validate({'file':'test','hsl':[{'band':'water'}]})

    def test_invalid_curve_rejected(self):
        curve=list(CURVE_X);curve[4]=float('nan')
        with self.assertRaises(ValueError):validate({'file':'test','curve_y':curve})

    def test_saturation_changes_stay_finite_and_in_gamut(self):
        pixels=np.random.default_rng(44).random((100,100,3),dtype=np.float32)
        plan=validate({'file':'test','basic':{'exposure':.5},'hsl':[{'band':'red','hue_shift':10,'saturation_percent':25,'lightness_points':7}]})
        result=apply_plan(pixels,plan)
        self.assertTrue(np.isfinite(result).all())
        self.assertGreaterEqual(result.min(),0);self.assertLessEqual(result.max(),1)

if __name__=='__main__':unittest.main()
