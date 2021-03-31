#!usr/bin/perl -w

use strict;

die "usage:perl $0 <mapping summary info list> \n" unless @ARGV == 1;

open A,$ARGV[0];
#print "Sample\tTotal_reads\tmapped_reads\tuniq_reads\tmapped_rate\tuniq_rate\n";
print "Sample\tTotal_reads\tmapped_reads\tmapped_rate(%)\n";
while(<A>){
    chomp;my @b=split /\s+/;
    next unless(-e $b[1]);
    open IN,$b[1];
    my ($total, $uniq, $multi, $other)=(0, 0 ,0 ,0);my $pe_maped;
    while(my $inf=<IN>){
        chomp $inf;my @a=split /\s+/,$inf;
        if($inf=~/Input/){
            $total+=$a[-1];
        }
        if($inf=~/Mapped/){
            $pe_maped+=$a[3];
        }
        if($inf=~/Aligned pairs/){
            $uniq=$a[-1];  
        }
        if($inf=~/have multiple alignments$/){
            $multi=$a[3];
        }
        if($inf=~/are discordant alignments/){
            $other=$a[1];
        }
    }
    close IN;
    $uniq=$uniq-$multi-$other;$uniq=2*$uniq;
	#print "$b[0]\t$total\t$pe_maped\t$uniq\t";
	print "$b[0]\t$total\t$pe_maped\t";
    printf "%.2f\n",100*$pe_maped/$total;
}
close A;
