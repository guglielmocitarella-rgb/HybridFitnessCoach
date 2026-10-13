import unittest
from matcher import assess, norm, valid_checkpoint

class MatchingTests(unittest.TestCase):
    def test_name_alone_is_not_valid_match(self):
        c={'canonicalNameEn':'Bench Press','equipmentStatus':'UNKNOWN'}
        s={'name':'Bench Press','equipment':['barbell'],'aliases':[]}
        self.assertEqual(assess(c,s)['match_type'],'MATCH AMBIGUO')
    def test_equipment_conflict_blocks_exact_name(self):
        c={'canonicalNameEn':'Bench Press','equipmentStatus':'KNOWN','equipmentRequired':['dumbbell']}
        s={'name':'Bench Press','equipment':['barbell'],'aliases':[]}
        self.assertEqual(assess(c,s)['match_type'],'MATCH AMBIGUO')
    def test_variant_not_erased(self):
        self.assertNotEqual(norm('Incline Bench Press'),norm('Bench Press'))
        c={'canonicalNameEn':'Incline Bench Press','equipmentStatus':'KNOWN','equipmentRequired':['barbell']}
        s={'name':'Bench Press','equipment':['barbell'],'aliases':[]}
        self.assertNotEqual(assess(c,s)['match_type'],'MATCH ALTAMENTE PROBABILE')
    def test_exact_name_and_equipment_is_probable_not_certain(self):
        c={'canonicalNameEn':'Barbell Bench Press','equipmentStatus':'KNOWN','equipmentRequired':['barbell']}
        s={'name':'Barbell Bench Press','equipment':['barbell'],'aliases':[]}
        self.assertEqual(assess(c,s)['match_type'],'MATCH ALTAMENTE PROBABILE')
    def test_corrupted_checkpoint_rejected(self):
        self.assertFalse(valid_checkpoint({'ids':['a'],'rows':[],'digest':'bad'},['a'],'snapshot'))

if __name__=='__main__': unittest.main()
