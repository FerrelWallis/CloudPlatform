#!usr/bin/perl -w

use strict;
use FindBin qw($Bin $Script);

die "usage:perl splice.pl <input.gtf> <tophat.junctions.bed> \n" unless @ARGV == 2;

my @a=split /\//,$ARGV[0];
my @b=split /\//,$ARGV[1];
#print "#PBS -l nodes=1:ppn=8\n#PBS -q public01\ncd \$PBS_O_WORKDIR\n";
print "cp  $ARGV[0] ./ \ncp $ARGV[1] ./\n";

print "perl $Bin/TransferFormat.pl       ./$a[-1]\n";
print "perl $Bin/AlternativeSplice_01.pl ./$a[-1] ./$b[-1] 0\n";
print "perl $Bin/AlternativeSplice_02.pl ./$a[-1].myformat ./$b[-1].novel\n";

print "awk 'BEGIN{getline; print \$0}\$6!~/Intergenic/ && \$9~/Coding/ && \$5 >= 2' ./$b[-1].novel.classify > ./$b[-1].NovelSplice.txt\n";
print "sed -n '2,\$p' ./$b[-1].NovelSplice.txt | awk '{print \$6}' | sort | uniq -c | awk '{print \$2\"\\t\"\$1}' > ./$b[-1].NovelSplice.stat\n";
