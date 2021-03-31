#!usr/bin/perl -w

use strict;

die "usage:perl $0 <exp.diff> <gene or isof> <output file pathway>\n" unless @ARGV == 3;

open A,$ARGV[0];    my $head=<A>;
my %sam;    my %fpkm;   my %all;
while(<A>){
    chomp;my @a=split;    $fpkm{$a[0]}{$a[4]}=$a[7];    $fpkm{$a[0]}{$a[5]}=$a[8];  $all{$a[4]}{$a[5]}{$.}=$_;
    $sam{$a[4]}=1;  $sam{$a[5]}=1;
}
close A;
my @sam=sort keys %sam; my $sam=join "\t",@sam;
print "ID\t$sam\n";
foreach my $k1(sort keys %fpkm){
    print "$k1";
    foreach my $k2(sort keys %sam){
        if(exists $fpkm{$k1}{$k2}){
            print "\t$fpkm{$k1}{$k2}";
        }
        else{
            print "\t0";
        }
    }
    print "\n";
}

foreach my $k1(sort keys %all){
    foreach my $k2(sort keys %{$all{$k1}}){
        open OUT,">$ARGV[2]/$k1.vs.$k2.$ARGV[1].diff";
        print OUT "$head";
        foreach my $k3(sort {$a<=>$b} keys %{$all{$k1}{$k2}}){
            print OUT "$all{$k1}{$k2}{$k3}\n";
        }
        close OUT;
    }
}
