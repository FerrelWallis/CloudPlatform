#!usr/bin/perl -w

use strict;
use FindBin qw($Bin $Script);

die "usage:perl $0 <fpkm.DEG.xls> <all.gene.list> <pathway.list> <GO.list> <output:ID> <human:yes or no>\n" unless @ARGV == 6;

open A,$ARGV[0]; open OUT2,">$ARGV[0].tr";	my %h;
open OO,">$ARGV[0].sh";

while(<A>){
    chomp;my @a=split;	print OUT2 "$a[0]\t1\n";    $h{$a[0]}=1;
}
close A;	close OUT2;
print OO "perl /lustre/sdb/zengl/bin/module/Denovo_rna/up_down_DEG.pl $ARGV[0] $ARGV[4]\n";
print OO "/lustre/sdb/taoye/miniconda3/envs/py2.7/bin/python /lustre/sdb/xueyj/pertest/goatools-0.5.7/scripts/find_enrichment.py $ARGV[4].all.lst $ARGV[1] $ARGV[3]  --fdr --alpha=0.05  > $ARGV[4].Go\n";
print OO "perl $Bin/enrich/filter_go_enrich.pl $ARGV[4].Go 0.05   $ARGV[4].Go.enrich.xls\n";
print OO "perl /lustre/sdb/zengl/bin/module/xueyj/go_enrich_bar.pl $ARGV[4].Go.enrich.xls $ARGV[0] > $ARGV[4].Go.bar.dat\n";
print OO "perl $Bin/tr_enrich_to_matlab.pl $ARGV[4].Go.enrich.xls 0.05 $ARGV[4].Go.enrich.xls.tr\n";
print OO "perl $Bin/DAG_figure.pl          $ARGV[0].tr  $ARGV[4].Go.enrich.xls.tr $ARGV[4].Go $ARGV[3].tr $ARGV[4].r\n";
print OO "perl $Bin/enrichment_plot_v3.pl  $ARGV[4].Go.enrich.xls go    $ARGV[4].Go.enrich 60\n";

print OO "perl /lustre/sdb/zengl/bin/module/kegg/kegg_enrich.pl $ARGV[0] $ARGV[2] /lustre/sdb/zengl/bin/module/kegg/K.ko.map.txt /lustre/sdb/zengl/bin/module/kegg/ko.layer.txt $ARGV[5] $ARGV[4]\n";
print OO "perl $Bin/enrichment_plot_v3.pl  $ARGV[4].Ko.enrich.xls kegg   $ARGV[4].Ko.enrich 60\n";
print OO "rm $ARGV[4].Go $ARGV[4]*xls.1 $ARGV[4]*xls.tr $ARGV[0].tr\n";
close OO;
`sh $ARGV[0].sh`;	`rm $ARGV[0].sh`;
